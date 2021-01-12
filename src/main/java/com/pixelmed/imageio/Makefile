OBJS = \
	JPEGLosslessImageReaderSpi.class \
	JPEGLosslessImageReader.class \
	JPEGLosslessMetadata.class \
	JPEGLosslessMetadataFormat.class \
	TestImageIO.class

all:	${OBJS}

PATHTOROOT = ../../..

include ${PATHTOROOT}/Makefile.common.mk

clean:
	rm -f *~ *.class core *.bak

testlosslessjpeg:
	# need to use jar file, since won't find new SPI in classpath without META-INF
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO /tmp/crap6.jpg jpeg-lossless 0
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO /tmp/crap6.jpg		# tests JPEGLosslessImageReaderSpi.canDecodeInput() (works irrespective of extension)
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO "$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process14/O1.JPG" jpeg-lossless 0	# 4 components, so will fail
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO "$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process14/O2.JPG" jpeg-lossless 0	# 4 components, so will fail
	#gunzip < "$${HOME}/Pictures/Medical/USFDigitalMammography/USFmammo_cases_partial_fromsite/DDSM/cases/benigns/benign_01/case0029/C_0029_1.LEFT_CC.LJPEG.gz" > /tmp/C_0029_1.LEFT_CC.LJPEG
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO /tmp/C_0029_1.LEFT_CC.LJPEG jpeg-lossless 0	# uses SV7
	#java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO /tmp/C_0029_1.LEFT_CC.LJPEG	# tests JPEGLosslessImageReaderSpi.canDecodeInput()
	rm -rf  /tmp/crap.jpg
	#dctoraw "$${HOME}/Pictures/Medical/JPEGLossless/BadWithPVRGCodecButOKWithOther/000caecb.dcm" /tmp/crap.jpg
	#dctoraw "$${HOME}/Pictures/Medical/JPEGLossless/BadWithPVRGCodecButOKWithOther/000cb12e.dcm" /tmp/crap.jpg
	dctoraw "$${HOME}/Pictures/Medical/JPEGLossless/EightBitJIIOCodecProblem/seq0" /tmp/crap.jpg
	#dctoraw "$${HOME}/Pictures/Medical/JPEGLossless/eightbitrgbsingleframe.dcm" /tmp/crap.jpg	# DHT segment between SOI and SOF
	#dctoraw "$${HOME}/Pictures/Medical/JPEGLossless/ivus_thousandsofframes_losslessjpeg.dcm" /tmp/crap.jpg	# DHT segment between SOI and SOF
	java -cp ${PATHTOROOT}/pixelmed_imageio.jar com.pixelmed.imageio.TestImageIO /tmp/crap.jpg
