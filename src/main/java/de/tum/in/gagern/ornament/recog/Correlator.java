package de.tum.in.gagern.ornament.recog;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Correlator {

    /* My x86 Linux has memory pages of 2^12 = 4096 bytes each.
     *
     * The following setting of tileBits = 5 gives a tile size where
     * each tile occupies exactly one page, as each float element
     * takes up four bytes. Using one page per tile should give the
     * most efficient cache performance.
     */
    private static final int tileBits = 5;
    private static final int tileSize = 1 << tileBits;
    private static final int tileMask = tileSize - 1;
    private static final int tileArea = tileSize * tileSize;

    private static final boolean doSymmetricColors = false;

    private final boolean auto;
    private final int wBits, hBits, wFFT, hFFT;
    private final float[] tileArray, omegas;
    private final float[] realArray, imagArray, realArray2, imagArray2;
    private final FloatBuffer accuBuf, realBuf1, imagBuf1, realBuf2, imagBuf2;
    private final int[] xBitrev, yBitrev;

    private int wImg;
    private int hImg;

    /*
     * Indexing in FloatBuffers is tile based. The index value
     * consists of these four parts, enumerated from least to most
     * significant bit:
     *
     * 1. the tileBits lower bits of the x index
     * 2. the tileBits lower bits of the y index
     * 3. the (wBits - tileBits) higher bits of the x index
     * 4. the (hBits - tileBits) higher bits of the y index
     *
     * The result is a memory layout consisting of tiles, each of
     * which is a square of size tileSize x tileSize. The pixels
     * within a tile as well as the tiles within the buffer are
     * addressed in row major mode, i.e. elements with the same y
     * coordinate and subsequent x coordinates follow one another.
     *
     * The reason for this tile based approach is in order to reduce
     * the impact of cache penalties when traversing the data in
     * column major mode.
     */

    public Correlator(int width, int height, boolean auto) throws IOException {
	wBits = Math.max(numBits(width), tileBits);
	hBits = Math.max(numBits(height), tileBits);
	this.auto = auto;
	if (wBits < tileBits || hBits < tileBits)
	    throw new IllegalArgumentException("FFT data sizes too small");
	if (wBits + hBits > 30)
	    throw new IllegalArgumentException("FFT data sizes too large");

	wFFT = 1 << wBits;
	hFFT = 1 << hBits;
	int maxFFT = Math.max(wFFT, hFFT);

	// allocate some arrays
	tileArray = new float[tileArea];
	realArray = new float[maxFFT];
	imagArray = new float[maxFFT];
	if (auto) {
	    realArray2 = null;
	    imagArray2 = null;
	}
	else {
	    realArray2 = new float[maxFFT];
	    imagArray2 = new float[maxFFT];
	}
	omegas = new float[maxFFT];

	// calculate omega values
	for (int i = 0; i < omegas.length; i += 2) {
	    double angle = Math.PI * i / omegas.length;
	    omegas[i] = (float)Math.cos(angle);
	    omegas[i+1] = (float)Math.sin(angle);
	}

	// calculate bit reversal arrays
	xBitrev = bitrevArray(wBits);
	yBitrev = bitrevArray(hBits);

	// create some float buffers, backed by a temporary file
	File tmpFile = File.createTempFile("orna", null);
	tmpFile.deleteOnExit();
	RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw");
	long bufSize = 4L * wFFT * hFFT;
	raf.setLength((auto ? 3 : 5)*bufSize);
	FileChannel chan = raf.getChannel();
	accuBuf = createBuf(chan, 0, bufSize);
	realBuf1 = createBuf(chan, bufSize, bufSize);
	imagBuf1 = createBuf(chan, 2*bufSize, bufSize);
	if (auto) {
	    realBuf2 = null;
	    imagBuf2 = null;
	}
	else {
	    realBuf2 = createBuf(chan, 3*bufSize, bufSize);
	    imagBuf2 = createBuf(chan, 4*bufSize, bufSize);
	}
	chan.close();
    }

    private FloatBuffer createBuf(FileChannel chan, long position, long size)
	    throws IOException {
	ByteBuffer byteBuf;
	byteBuf = chan.map(FileChannel.MapMode.READ_WRITE, position, size);
	byteBuf.order(ByteOrder.nativeOrder());
	return byteBuf.asFloatBuffer();
    }

    public void correlate(BufferedImage img1, BufferedImage img2,
			  ProgressPhase progress)
	    throws CanceledOperationException {
	if (auto)
	    throw new IllegalArgumentException("autocorrelations only");
	int numBands = img1.getSampleModel().getNumBands();
	if (img2.getSampleModel().getNumBands() != numBands)
	    throw new IllegalArgumentException("different number of bands");
	ProgressPhase[] progressBands = new ProgressPhase[numBands];
	for (int band = 0; band < numBands; ++band)
	    progressBands[band] = progress.createPhase(1);
	wImg = Math.max(img1.getWidth(), img2.getWidth());
	hImg = Math.max(img1.getHeight(), img2.getHeight());
	progress.begin();
	zero(accuBuf);
	for (int band = 0; band < numBands; ++band) {
	    ProgressPhase progressBand = progressBands[band];
	    ProgressPhase progressRead1 = progressBand.createPhase(1.f);
	    ProgressPhase progressRead2 = progressBand.createPhase(1.f);
	    ProgressPhase progressFFT1  = progressBand.createPhase(10.f);
	    ProgressPhase progressFFT2  = progressBand.createPhase(10.f);
	    ProgressPhase progressMult  = progressBand.createPhase(4.f);
	    ProgressPhase progressIFFT  = progressBand.createPhase(10.f);
	    ProgressPhase progressAccu  = progressBand.createPhase(3.f);
	    progressBand.setNoticeSuffix(" band " + (band+1));
	    progressRead1.setNoticeSuffix(" image 1");
	    progressRead2.setNoticeSuffix(" image 2");
	    progressFFT1.setNoticeSuffix(" image 1");
	    progressFFT2.setNoticeSuffix(" image 2");

	    read(img1, band, realBuf1, imagBuf1, progressRead1);
	    read(img2, band, realBuf2, imagBuf2, progressRead2);
	    fft2D(realBuf1, imagBuf1, false, progressFFT1);
	    fft2D(realBuf2, imagBuf2, false, progressFFT2);
	    multiply(progressMult);
	    fft2D(realBuf1, imagBuf1, true, progressIFFT);
	    accumulate(progressAccu);
	}
	progress.done();
    }

    public void autocorrelate(BufferedImage img, ProgressPhase progress)
	    throws CanceledOperationException {
	int numBands = img.getSampleModel().getNumBands();
	ProgressPhase[] progressBands = new ProgressPhase[numBands];
	for (int band = 0; band < numBands; ++band)
	    progressBands[band] = progress.createPhase(1);
	wImg = img.getWidth();
	hImg = img.getHeight();
	progress.begin();
	zero(accuBuf);
	for (int band = 0; band < numBands; ++band) {
	    ProgressPhase progressBand = progressBands[band];
	    ProgressPhase progressRead = progressBand.createPhase(1.f);
	    ProgressPhase progressFFT  = progressBand.createPhase(10.f);
	    ProgressPhase progressMult = progressBand.createPhase(3.f);
	    ProgressPhase progressIFFT = progressBand.createPhase(10.f);
	    ProgressPhase progressAccu = progressBand.createPhase(3.f);
	    progressBand.setNoticeSuffix(" band " + (band+1));

	    progressBand.begin();
	    read(img, band, realBuf1, imagBuf1, progressRead);
	    fft2D(realBuf1, imagBuf1, false, progressFFT);
	    multiplyAuto(progressMult);
	    fft2D(realBuf1, imagBuf1, true, progressIFFT);
	    accumulate(progressAccu);
	    progressBand.done();
	}
	progress.done();
    }

    private void read(BufferedImage img, int band,
		      FloatBuffer realBuf, FloatBuffer imagBuf,
		      ProgressPhase progress)
	    throws CanceledOperationException {
	int wImg = img.getWidth(), hImg = img.getHeight();
	if (wImg > wFFT || hImg > hFFT)
	    throw new IllegalArgumentException("image too large");
	progress.begin("reading", numTiles(wImg)*numTiles(hImg));
	Raster raster = img.getRaster();
	zero(realBuf);
	zero(imagBuf);

	// read samples from raster in tile-based traversal
	for (int y = 0; y < hImg; y += tileSize) {
	    realBuf.position(y*wFFT);
	    for (int x = 0; x < wImg; x += tileSize) {
		int w = wImg - x, h = hImg - y;
		if (h >= tileSize) h = tileSize;
		else zero(tileArray);
		if (w >= tileSize) w = tileSize;
		raster.getSamples(x, y, w, h, band, tileArray);
		if (w < tileSize) {
		    int pFrom = h*w, pTo = h*tileSize;
		    while (pTo > 0) {
			for (int x2 = w; x2 < tileSize; ++x2)
			    tileArray[--pTo] = 0.f;
			for (int x2 = 0; x2 < w; ++x2)
			    tileArray[--pTo] = tileArray[--pFrom];
		    }
		}
		if (doSymmetricColors) {
		    for (int i = 0; i < tileArea; ++i)
			tileArray[i] -= 127.5f; // symmetric color distribution
		}
		realBuf.put(tileArray);
		progress.step();
	    }
	}
	progress.done();
    }

    private void multiply(ProgressPhase progress)
	    throws CanceledOperationException {
	progress.begin("combining", realBuf1.limit() / realArray.length);
	realBuf1.rewind();
	imagBuf1.rewind();
	realBuf2.rewind();
	imagBuf2.rewind();
	while (realBuf1.hasRemaining()) {
	    realBuf1.mark();
	    imagBuf1.mark();
	    realBuf1.get(realArray);
	    imagBuf1.get(imagArray);
	    realBuf2.get(realArray2);
	    imagBuf2.get(imagArray2);
	    for (int i = 0; i < realArray.length; ++i) {
		float real1 = realArray[i], imag1 = imagArray[i];
		float real2 = realArray2[i], imag2 = imagArray2[i];
		realArray[i] = real1*real2 + imag1*imag2;
		imagArray[i] = real1*imag2 - real2*imag1;
	    }
	    realBuf1.reset();
	    realBuf1.put(realArray);
	    imagBuf1.reset();
	    imagBuf1.put(imagArray);
	    progress.step();
	}
	progress.done();
    }

    private void multiplyAuto(ProgressPhase progress)
	    throws CanceledOperationException {
	progress.begin("multiplying", realBuf1.limit() / realArray.length);
	realBuf1.rewind();
	imagBuf1.rewind();
	while (realBuf1.hasRemaining()) {
	    realBuf1.mark();
	    realBuf1.get(realArray);
	    imagBuf1.get(imagArray);
	    for (int i = 0; i < realArray.length; ++i) {
		float real = realArray[i], imag = imagArray[i];
		realArray[i] = real*real + imag*imag;
	    }
	    realBuf1.reset();
	    realBuf1.put(realArray);
	    progress.step();
	}
	zero(imagBuf1);
	progress.done();
    }

    private void accumulate(ProgressPhase progress)
	    throws CanceledOperationException {
	progress.begin("accumulating", accuBuf.limit() / realArray.length);
	accuBuf.rewind();
	realBuf1.rewind();
	while (accuBuf.hasRemaining()) {
	    accuBuf.mark();
	    accuBuf.get(imagArray);
	    realBuf1.get(realArray);
	    for (int i = 0; i < realArray.length; ++i) {
		// accumulation function: add real values
		imagArray[i] += realArray[i];
	    }
	    accuBuf.reset();
	    accuBuf.put(imagArray);
	    progress.step();
	}
	progress.done();
    }

    private void fft2D(final FloatBuffer realBuf, final FloatBuffer imagBuf,
		       final boolean inverse, ProgressPhase progress)
	    throws CanceledOperationException {
	ProgressPhase progress1 = progress.createPhase(1);
	ProgressPhase progress2 = progress.createPhase(1);
	progress.begin(inverse ? "inverse transforming" : "transforming");
	fft2Dim1Dir(realBuf, imagBuf,
		    wFFT, tileBits, 0,
		    hFFT, wBits, tileBits,
		    hBits, yBitrev, inverse,
		    progress1);
	fft2Dim1Dir(realBuf, imagBuf,
		    hFFT, wBits, tileBits,
		    wFFT, tileBits, 0,
		    wBits, xBitrev, inverse,
		    progress2);
	progress.done();
    }

    private void fft2Dim1Dir(final FloatBuffer realBuf,
			     final FloatBuffer imagBuf,
			     final int aCount, final int aHigh, final int aLow,
			     final int bCount, final int bHigh, final int bLow,
			     final int bBits, final int[] bitrev,
			     final boolean inverse, ProgressPhase progress)
	    throws CanceledOperationException {
	progress.begin(aCount);
	for (int a = 0; a < aCount; ++a) {
	    int aPos = ((a & ~tileMask) << aHigh) | ((a & tileMask) << aLow);
	    for (int b = 0; b < bCount; ++b) {
		int pos = ((b & ~tileMask) << bHigh) | ((b & tileMask) << bLow)
		          | aPos;
		realArray[bitrev[b]] = realBuf.get(pos);
		imagArray[bitrev[b]] = imagBuf.get(pos);
	    }
	    fft1D(bBits, inverse);
	    for (int b = 0; b < bCount; ++b) {
		int pos = ((b & ~tileMask) << bHigh) | ((b & tileMask) << bLow)
		          | aPos;
		realBuf.put(pos, realArray[b]);
		imagBuf.put(pos, imagArray[b]);
	    }
	    progress.step();
	}
	progress.done();
    }

    private void fft1D(final int bits, final boolean inverse) {
	int count = 1 << bits;
	int bitPower = 1;
	int omegaStep = omegas.length;

	for (int bit = 0; bit < bits; ++bit) {
	    int omegaIndex = 0;
	    for (int omegaPower = 0; omegaPower < bitPower; ++omegaPower) {
		float omegaReal = omegas[omegaIndex];
		float omegaImag = omegas[omegaIndex+1]*(inverse ? -1.f : 1.f);
		int i = omegaPower;
		while (i < count) {
		    int j = i + bitPower;
		    float evenReal = realArray[i], oddReal = realArray[j];
		    float evenImag = imagArray[i], oddImag = imagArray[j];
		    float deltaReal = oddReal*omegaReal - oddImag*omegaImag;
		    float deltaImag = oddImag*omegaReal + oddReal*omegaImag;
		    realArray[j] = evenReal - deltaReal;
		    realArray[i] = evenReal + deltaReal;
		    imagArray[j] = evenImag - deltaImag;
		    imagArray[i] = evenImag + deltaImag;
		    i = j + bitPower;
		}
		omegaIndex += omegaStep;
	    }
	    bitPower <<= 1;
	    omegaStep >>= 1;
	}
    }

    /**
     * Calculate the number of bits for given range of values.
     * The result is ceil(lg(n)), that is the base two logarithm
     * of the argument, rounded up to the next integer.
     * The returned number is the smallest number of bits sufficient
     * to express all values from 0 through (n-1).
     */
    private static int numBits(int n) {
	int bits;
	for (bits = 0; (1 << bits) < n; ++bits);
	return bits;
    }

    private static int[] bitrevArray(int bits) {
	int[] a = new int[1 << bits];
	for (int i = 0; i < a.length; ++i) {
	    int in = i, out = 0;
	    for (int k = 0; k < bits; ++k) {
		out = (out << 1) | (in & 1);
		in >>= 1;
	    }
	    a[i] = out;
	}
	return a;
    }

    private static void zero(float[] array) {
	Arrays.fill(array, 0.f);
    }

    private void zero(FloatBuffer buf) {
	zero(tileArray);
	buf.clear();
	while (buf.hasRemaining())
	    buf.put(tileArray);
	buf.clear();
    }

    private static int roundUpToTileSize(int i) {
	return ((i - 1) | tileMask) + 1;
    }

    private static int numTiles(int i) {
	return (i - 1)/tileSize + 1;
    }

    public BufferedImage getDebugImage(boolean gray, boolean fullArea,
				       float blackPoint, float whitePoint,
				       ProgressPhase progress)
	    throws CanceledOperationException {
	int wRes = roundUpToTileSize(wImg);
	int hRes = roundUpToTileSize(hImg);
	if (fullArea) {
	    wRes = wFFT/2;
	    hRes = hFFT/2;
	}
	if (progress != null)
	    progress.begin("creating debug image", 4*wRes*hRes/tileArea);
	int mode;
	if (gray) mode = BufferedImage.TYPE_BYTE_GRAY;
	else mode = BufferedImage.TYPE_INT_RGB;
	BufferedImage res = new BufferedImage(2*wRes, 2*hRes, mode);
	if (whitePoint <= 0) {
	    progress.setMaximum(8*wRes*hRes/tileArea);
	    blackPoint = Float.POSITIVE_INFINITY;
	    for (int yRes = 0; yRes < 2*hRes; yRes += tileSize) {
		int yPos = ((yRes + hFFT - hRes)%hFFT) << wBits;
		for (int xRes = 0; xRes < 2*wRes; xRes += tileSize) {
		    int pos = yPos | (((xRes + wFFT - wRes)%wFFT) << tileBits);
		    accuBuf.position(pos);
		    accuBuf.get(tileArray);
		    for (int i = 0; i < tileArray.length; ++i) {
			if (whitePoint < tileArray[i])
			    whitePoint = tileArray[i];
			if (blackPoint > tileArray[i])
			    blackPoint = tileArray[i];
		    }
		    progress.step();
		}
	    }
	}
	else if (blackPoint < 0 && !doSymmetricColors)
	    blackPoint = 0;
	float scale = 1.f/(whitePoint - blackPoint);
	int[] intArray = new int[tileArea];
	for (int yRes = 0; yRes < 2*hRes; yRes += tileSize) {
	    int yPos = ((yRes + hFFT - hRes)%hFFT) << wBits;
	    for (int xRes = 0; xRes < 2*wRes; xRes += tileSize) {
		int pos = yPos | (((xRes + wFFT - wRes)%wFFT) << tileBits);
		accuBuf.position(pos);
		accuBuf.get(tileArray);
		for (int i = 0; i < tileArray.length; ++i) {
		    float level = scale*(tileArray[i] - blackPoint);
		    if (level < 0) level = 0;
		    if (level > 1) level = 1;
		    if (gray) intArray[i] = Color.HSBtoRGB(0.f, 0.f, level);
		    else intArray[i] = Color.HSBtoRGB(2/3.f*(1.f-level),
						      1.f, .3f+.7f*level);
		}
		res.setRGB(xRes, yRes, tileSize, tileSize,
			   intArray, 0, tileSize);
		if (progress != null)
		    progress.step();
	    }
	}
	if (!fullArea)
	    res = res.getSubimage(1+wRes-wImg, 1+hRes-hImg, wImg*2-1, hImg*2-1);
	if (progress != null)
	    progress.done();
	return res;
    }

    public float normalize() {
	float minValue = Float.POSITIVE_INFINITY;
	int wRound = roundUpToTileSize(wImg), hRound = roundUpToTileSize(hImg);
	for (int y1 = -hRound; y1 < hImg; y1 += tileSize) {
	    int yPos = ((y1 + hFFT)%hFFT) << wBits;
	    for (int x1 = -wRound; x1 < wImg; x1 += tileSize) {
		int pos = yPos | (((x1 + wFFT)%wFFT) << tileBits);
		accuBuf.position(pos);
		accuBuf.get(tileArray);
		int index = 0;
		for (int y2 = 0; y2 < tileSize; ++y2) {
		    int y = y1 + y2;
		    y = hImg + (y < 0 ? y : -y);
		    if (y <= 0) y = 1;
		    for (int x2 = 0; x2 < tileSize; ++x2) {
			int x = x1 + x2;
			x = wImg + (x < 0 ? x : -x);
			float value = tileArray[index]/(x*y);
			if (x <= 0 || y <= 0) value = 0;
			else if (minValue > value) minValue = value;
			tileArray[index] = value;
			++index;
		    }
		}
		accuBuf.position(pos);
		accuBuf.put(tileArray);
	    }
	}
	return minValue;
    }

    /**
     * Factor limiting the number of peaks in the initial peak extraction.
     *
     * In order to limit the number of peaks the area of dominance
     * calculation has to deal with, not every pixel is considered a
     * local peak. Instead the plane is divided into a grid of
     * squares, and only the maximum value from each square is treated
     * as a local peak.
     *
     * The grid cells have a power of two edge length, which is
     * derived from the image width or height, whichever is greater. A
     * value of 9 will give grid cells of 4x4 pixels if the image is
     * no bigger than 1024x1024 pixels and at least one size is
     * greater than 512 pixels. This will limit the total number of
     * peaks to 65536. An decrease of this constant by one will
     * decrease the maximum number of peaks by a factor of four.
     */
    private static final int peakGridShift = 8;

    public List extractPeaks(float threshold, ProgressPhase progress)
	    throws CanceledOperationException, RecognitionException {
	progress.begin("extracting peaks", numTiles(wImg)*numTiles(hImg)*4);
	int gridBits = Math.max(wBits, hBits) - peakGridShift;
	if (gridBits < 0) gridBits = 0;
	if (gridBits > tileBits) gridBits = tileBits;
	int gridSize = 1 << gridBits;
	System.err.println("gridSize = " + gridSize);
	int wRound = roundUpToTileSize(wImg), hRound = roundUpToTileSize(hImg);
	List peaks = new ArrayList();
	for (int y1 = -hRound; y1 < hImg; y1 += tileSize) {
	    int yPos = ((y1 + hFFT)%hFFT) << wBits;
	    for (int x1 = -wRound; x1 < wImg; x1 += tileSize) {
		int pos = yPos | (((x1 + wFFT)%wFFT) << tileBits);
		accuBuf.position(pos);
		accuBuf.get(tileArray);
		for (int y2 = 0; y2 < tileSize; y2 += gridSize) {
		    for (int x2 = 0; x2 < tileSize; x2 += gridSize) {
			int pos2 = (y2 << tileBits) | x2;
			float maxValue = threshold;
			int maxX = 0, maxY = 0;
			for (int y3 = 0; y3 < gridSize; ++y3) {
			    int pos3 = pos2 | (y3 << tileBits);
			    for (int x3 = 0; x3 < gridSize; ++x3) {
				float value = tileArray[pos3 + x3];
				if (maxValue >= value) continue;
				maxValue = value;
				maxX = x3;
				maxY = y3;
			    }
			}
			if (maxValue == threshold) continue;
			int px = x1 + x2 + maxX, py = y1 + y2 + maxY;
			if (Math.abs(px) >= wImg || Math.abs(py) >= hImg)
			    continue;
			peaks.add(new Peak(px, py, maxValue, wImg, hImg));
		    }
		}
		progress.step();
	    }
	}
	progress.done();
	return peaks;
    }

    public Peak getMaximum() {
	return getExtremum(1);
    }

    public Peak getMinimum() {
	return getExtremum(-1);
    }

    private Peak getExtremum(float direction) {
	float bestValue = Float.NEGATIVE_INFINITY;
	int bestX = 0;
	int bestY = 0;
	accuBuf.rewind();
	for (int y1 = 0; y1 < hFFT; y1 += tileSize) {
	    for (int x1 = 0; x1 < wFFT; x1 += tileSize) {
		accuBuf.get(tileArray);
		for (int i = 0; i < tileArea; ++i) {
		    float value = tileArray[i]*direction;
		    if (bestValue >= value) continue;
		    int x = x1 + (i & tileMask), y = y1 + (i >> tileBits);
		    if (x >= wImg) x -= wFFT;
		    if (y >= hImg) y -= hFFT;
		    if (-x >= wImg || -y >= hImg) continue;
		    bestValue = value;
		    bestX = x;
		    bestY = y;
		}
	    }
	}
	return new Peak(bestX, bestY, bestValue/direction, wImg, hImg);
    }

    public float getAcMaximum() {
	return accuBuf.get(0);
    }

    public int getWidth() {
	return wFFT;
    }

    public int getHeight() {
	return hFFT;
    }

}
