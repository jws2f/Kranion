OBJS = \
	EntropyCodedSegment.class \
	HuffmanTable.class \
	Markers.class \
	MarkerSegmentAPP0JFIF.class \
	MarkerSegmentDHT.class \
	MarkerSegmentDQT.class \
	MarkerSegmentSOF.class \
	MarkerSegmentSOS.class \
	Parse.class \
	QuantizationTable.class \
	Utilities.class \
	OutputArrayOrStream.class

all:	${OBJS}

PATHTOROOT = ../../../..

include ${PATHTOROOT}/Makefile.common.mk

clean:
	rm -f *~ *.class core *.bak

testparse:	${OBJS}
	rm -f /tmp/crap_copied.jpg
	rm -f /tmp/crap_source.jpg
	cp -v \
		"$${HOME}/Documents/Clients/MDDX/Experiment20130905/crap.jpg" \
		/tmp/crap_source.jpg
	java -Djava.awt.headless=true  -cp ${PATHTOROOT} com.pixelmed.codec.jpeg.Parse \
		/tmp/crap_source.jpg \
		/tmp/crap_copied.jpg
	@echo "Comparing source and copied ... may fail with EOF if padding after EOI marker that is not copied, which is OK"
	cmp /tmp/crap_source.jpg /tmp/crap_copied.jpg
	@echo "Finished comparing"
	hexdump -C /tmp/crap_source.jpg | tail -3
	hexdump -C /tmp/crap_copied.jpg | tail -3

# without restart and working
		#"$${HOME}/Documents/Medical/compression/JPEG/10918-1/jpeg-6/testimg.jpg"
		#"${PATHTOROOT}/${PATHTOTESTFILESFROMROOT}/colorpngresavedfrompreview.jpg"
		#"${PATHTOROOT}/${PATHTOTESTFILESFROMROOT}/huff_simple0.jpg"
		#"$${HOME}/Documents/Clients/MDDX/Experiment20130905/crap.jpg"
		#"${PATHTOROOT}/${PATHTOTESTFILESFROMROOT}/smpte_8_q1.jpg"

# without restart and not working

# with restart and working
		#"$${HOME}/Pictures/Interesting/clunie_737_cropped_close.jpg"
		#"${PATHTOROOT}/${PATHTOTESTFILESFROMROOT}/smpte_8_cjpeg_rst1.jpg"
		#"/tmp/crap.jpg"
		#"$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process1/A1.JPG"

# with restart and not working



testdecompress:	${OBJS}
	rm -f /tmp/crap_source.jpg
	rm -f /tmp/crap_decompressed*.raw
	cp -v \
		#"$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process14/O1.JPG" \
		/tmp/crap_source.jpg
	java -Djava.awt.headless=true  -cp ${PATHTOROOT} com.pixelmed.codec.jpeg.Parse \
		/tmp/crap_source.jpg \
		"" \
		/tmp/crap_decompressed.raw

		#"$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process14/O1.JPG" \
		#"$${HOME}/Documents/Medical/compression/JPEG/10918-2/ITU T83/T83_process14/O2.JPG" \

testdecompressfromdicom:	${OBJS}
	rm -f /tmp/crap_source.jpg
	rm -f /tmp/crap_decompressed*.raw
	dctoraw \
		"$${HOME}/Pictures/Medical/JPEGVarious/z18" \
		/tmp/crap_source.jpg
	java -Djava.awt.headless=true  -cp ${PATHTOROOT} com.pixelmed.codec.jpeg.Parse \
		/tmp/crap_source.jpg \
		"" \
		/tmp/crap_decompressed.raw
