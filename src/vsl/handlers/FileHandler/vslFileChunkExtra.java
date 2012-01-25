package vsl.handlers.FileHandler;

import vsl.core.data.*;

import java.io.Serializable;

public class vslFileChunkExtra extends vslChunkHeaderExtra implements Serializable {

	int chunkNum = -1;
	int tokenSize = -1;
	byte[] beginToken;
	byte[] endToken;

}
