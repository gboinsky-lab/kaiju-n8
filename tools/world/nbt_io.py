"""Escritor minimo de NBT (gzip, big-endian) para os templates de estrutura; sem dependencia externa (nbtlib).

Tipos: dict -> Compound, str -> String, bool -> Byte, int -> Int, float -> Double, e as marcas Byte/Short/Long/
Float/IntList/DoubleList/CompoundList para o resto. Listas vazias precisam de marca (o tipo do elemento vai no arquivo).
"""
import gzip
import struct


class Byte(int):
    pass


class Float(float):
    pass


class Typed(list):
    """Lista com o tipo do elemento explicito."""
    tag = 0

    def __init__(self, items=()):
        super().__init__(items)


class IntList(Typed):
    tag = 3


class DoubleList(Typed):
    tag = 6


class CompoundList(Typed):
    tag = 10


def _tag_of(value):
    if isinstance(value, bool) or isinstance(value, Byte):
        return 1
    if isinstance(value, int):
        return 3
    if isinstance(value, Float):
        return 5
    if isinstance(value, float):
        return 6
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError(type(value))


def _payload(out, value):
    tag = _tag_of(value)
    if tag == 1:
        out += struct.pack(">b", int(value))
    elif tag == 3:
        out += struct.pack(">i", value)
    elif tag == 5:
        out += struct.pack(">f", value)
    elif tag == 6:
        out += struct.pack(">d", value)
    elif tag == 8:
        data = value.encode("utf-8")
        out += struct.pack(">H", len(data)) + data
    elif tag == 9:
        element = value.tag if isinstance(value, Typed) else (_tag_of(value[0]) if value else 0)
        out += struct.pack(">bi", element, len(value))
        for item in value:
            _payload(out, item)
    elif tag == 10:
        for key, item in value.items():
            name = key.encode("utf-8")
            out += struct.pack(">bH", _tag_of(item), len(name)) + name
            _payload(out, item)
        out += b"\x00"


def write(path, root):
    out = bytearray(b"\x0a\x00\x00")
    _payload(out, root)
    # mtime 0: regerar sem mudar nada da o mesmo arquivo (sem diff a toa no Git).
    with open(path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as handle:
        handle.write(bytes(out))
