package com.mycz.krpc.core.remoting.transport.compress;

public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);

}
