// src/main/java/com/kn8/common/data/DataRegistry.java
package com.kn8.common.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kn8.KN8Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Um tipo de dado do mod carregado de JSON por reload listener + Codec (decisao do PT2). Arquivos em
 * {@code data/<namespace>/kn8/<nome>/*.json}.
 *
 * <p>Tres "fotos" imutaveis, trocadas inteiras:</p>
 * <ul>
 *   <li><b>loaded</b>: o que o Codec aceitou no ultimo carregamento (arquivo invalido = log + ignorado);</li>
 *   <li><b>server</b>: o que passou tambem pela validacao de referencias cruzadas ({@link DataValidation});</li>
 *   <li><b>client</b>: o que este cliente recebeu do servidor ({@link DataSyncS2C}).</li>
 * </ul>
 * Por que estatico: os recursos carregam antes de existir o {@code KN8Server}; as fotos sao limpas ao parar o
 * servidor (servidor) e ao desconectar (cliente), entao nada vaza entre mundos.
 */
public final class DataRegistry<T> {

    private static final Gson GSON = new GsonBuilder().create();

    private final ResourceLocation id;
    private final String directory;
    private final Codec<T> codec;
    private final Codec<Map<ResourceLocation, T>> mapCodec;
    private final boolean syncToClient;

    private volatile Map<ResourceLocation, T> loaded = Map.of();
    private volatile Map<ResourceLocation, T> server = Map.of();
    private volatile Map<ResourceLocation, T> client = Map.of();
    private volatile List<String> parseErrors = List.of();
    // Sobem a cada nova foto (validacao no servidor, recebimento no cliente): quem guarda valores derivados dos dados
    // (ex.: atributos de um kaiju) compara a versao para saber que precisa recalcular depois de um /reload.
    private volatile int serverVersion;
    private volatile int clientVersion;

    DataRegistry(String name, Codec<T> codec, boolean syncToClient) {
        this.id = KN8Constants.id(name);
        this.directory = KN8Constants.MOD_ID + "/" + name;
        this.codec = codec;
        this.mapCodec = Codec.unboundedMap(ResourceLocation.CODEC, codec);
        this.syncToClient = syncToClient;
    }

    public ResourceLocation id() {
        return id;
    }

    /** Pasta dentro de {@code data/<namespace>/}, ex.: {@code kn8/kaiju}. */
    public String directory() {
        return directory;
    }

    public boolean syncToClient() {
        return syncToClient;
    }

    /** Definicoes validadas no servidor. */
    public Map<ResourceLocation, T> server() {
        return server;
    }

    /** Definicoes recebidas por este cliente (vazio se o registry nao e sincronizado). */
    public Map<ResourceLocation, T> client() {
        return client;
    }

    /** Lado certo para quem tem um {@code Level}: {@code forSide(level.isClientSide())}. */
    public Map<ResourceLocation, T> forSide(boolean clientSide) {
        return clientSide ? client : server;
    }

    public Optional<T> get(ResourceLocation key, boolean clientSide) {
        return Optional.ofNullable(forSide(clientSide).get(key));
    }

    /** Versao da foto de um lado; muda sempre que a foto e trocada. */
    public int version(boolean clientSide) {
        return clientSide ? clientVersion : serverVersion;
    }

    /** Decodifica um JSON; publico para testes de erro (GameTests). */
    public DataResult<T> parse(JsonElement json) {
        return codec.parse(JsonOps.INSTANCE, json);
    }

    Map<ResourceLocation, T> loaded() {
        return loaded;
    }

    /** Arquivos ignorados no ultimo carregamento (caminho + motivo), para o relatorio do {@code /kn8 data validate}. */
    List<String> parseErrors() {
        return parseErrors;
    }

    /**
     * Mensagem de erro do Codec sem o trecho "missed input: {...}" que o DFU acrescenta (despeja o JSON inteiro e
     * esconde o motivo). Publico para os GameTests.
     */
    public static String cleanMessage(String message) {
        int cut = message.indexOf("missed input");
        String clean = cut < 0 ? message : message.substring(0, cut);
        return clean.replaceAll("[\\s;:,]+$", "").trim();
    }

    void publishValidated(Map<ResourceLocation, T> validated) {
        server = Map.copyOf(validated);
        serverVersion++;
    }

    void clearServer() {
        loaded = Map.of();
        server = Map.of();
        parseErrors = List.of();
    }

    void clearClient() {
        client = Map.of();
    }

    /** Codifica a foto do servidor para envio ao cliente. */
    Tag encodeForClient() {
        return mapCodec.encodeStart(NbtOps.INSTANCE, server).getOrThrow();
    }

    /** Recebe a foto do servidor no cliente. Dado corrompido vira log, nunca crash. */
    void acceptFromServer(Tag tag) {
        DataResult<Map<ResourceLocation, T>> result = mapCodec.parse(NbtOps.INSTANCE, tag);
        result.resultOrPartial(error -> KN8Constants.LOGGER.error("[kn8] Dados {} recebidos com erro: {}", id, error))
                .ifPresent(map -> {
                    client = Map.copyOf(map);
                    clientVersion++;
                });
    }

    PreparableReloadListener createReloadListener() {
        return new SimpleJsonResourceReloadListener(GSON, directory) {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
                    ProfilerFiller profiler) {
                loadFiles(files);
            }
        };
    }

    private void loadFiles(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, T> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            DataResult<T> parsed = parse(file.getValue());
            Optional<T> value = parsed.result();
            if (value.isPresent()) {
                result.put(file.getKey(), value.get());
            } else {
                String reason = cleanMessage(parsed.error().map(DataResult.Error::message)
                        .orElse("erro desconhecido"));
                String path = "data/" + file.getKey().getNamespace() + "/" + directory + "/" + file.getKey().getPath()
                        + ".json";
                errors.add(path + " ignorado: " + reason);
                KN8Constants.LOGGER.error("[kn8] {} ignorado: {}", path, reason);
            }
        }
        parseErrors = List.copyOf(errors);
        loaded = Map.copyOf(result);
        // Antes da validacao cruzada o servidor ja ve o que o Codec aceitou; a validacao refina em seguida.
        server = loaded;
        KN8Constants.LOGGER.info("[kn8] {}: {} carregado(s), {} arquivo(s) com erro.", id, result.size(),
                errors.size());
    }

    /** Lista so de leitura, para comandos. */
    public List<ResourceLocation> serverIds() {
        return server.keySet().stream().sorted().toList();
    }
}
