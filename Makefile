#
# Makefile, part of Equivalence
#
# Copyright (c) 2006 Operational Dynamics Consulting Pty Ltd 
# 
# The code in this file, and the library it is a part of, are made available
# to you by the authors under the terms of the "GNU General Public Licence,
# version 2". See the LICENCE file for the terms governing usage and
# redistribution.
#

ifdef V
else
MAKEFLAGS=-s
endif

-include .config

ifdef GCJ
all: build-java build-native
else
all: build-java
endif

build-java: tmp/gtk-$(APIVERSION).jar tmp/libgtkjni-$(APIVERSION).so

build-native: tmp/libgtkjava-$(APIVERSION).so

.PHONY: doc clean

# [this  will be called by the above include if .config is missing.
# We don't call ./configure automatically to allow scope for
# manual configuration and overrides]
.config: src/java/org/gnome/gtk/Gtk.java
	echo
	echo "You need to run ./configure to check prerequisites"
	echo "and setup preferences before you can build accounts."
	( if [ ! -x configure ] ; then chmod +x configure ; echo "I just made it executable for you." ; fi )
	echo
	exit 1

build/config: .config build/dirs
	@echo "CHECK     build system configuration"
	( if [ ! "$(JAVA_CMD)" ] ; then echo "Sanity check failed. Run ./configure" ; exit 1 ; fi )
	touch $@

SOURCES_JAVA=$(shell find src/java -name '*.java') $(shell find mockup/java -name '*.java')

CLASSES_JAVA=$(shell echo $(SOURCES_JAVA) | sed -e's/\.java/\.class/g' -e's/src\/java/tmp\/classes/g' -e's/mockup\/java/tmp\/classes/g')

# These are just the headers which are crafted, not generated
HEADERS_C=$(shell find src/jni -name '*.h' | sed -e 's/src\/jni/tmp\/include/g' -e 's/\.c/\.h/g')

SOURCES_C=$(shell find src/java -name '*.c' ) $(shell find mockup/java -name '*.c' )
OBJECTS_C=$(shell echo $(SOURCES_C) | sed -e's/\.c/\.o/g' -e's/src\/java/tmp\/objects/g' -e's/mockup\/java/tmp\/objects/g' )

SOURCES_GLUE=$(shell find src/jni -name '*.c' )
OBJECTS_GLUE=$(shell echo $(SOURCES_GLUE) | sed -e's/\.c/\.o/g' -e's/src\/jni/tmp\/objects/g' )

#
# convenience target: setup pre-reqs
#
build/dirs: .config
	@echo "MKDIR     temporary build directories"
	-test -d build || mkdir build
	-test -d tmp/classes || mkdir -p tmp/classes
	-test -d tmp/native || mkdir -p tmp/native
	-test -d tmp/objects || mkdir -p tmp/objects
	-test -d tmp/include || mkdir -p tmp/include
	touch $@


# --------------------------------------------------------------------
# Source compilation
# --------------------------------------------------------------------

tmp/gtk-$(APIVERSION).jar: build/config build/classes
	@echo "$(JAR_CMD) $@"
	cd tmp/classes ; find . -name '*.class' | xargs $(JAR) cf ../../$@ 

build/classes: $(SOURCES_JAVA)
	@echo "$(JAVAC_CMD) tmp/classes/*.class"
	$(JAVAC) -d tmp/classes -classpath $(JAVAGNOME_JARS):src/java:tmp/classes $?
	touch $@


GTK_CFLAGS=$(shell pkg-config --cflags gthread-2.0) \
		$(shell pkg-config --cflags glib-2.0) \
		$(shell pkg-config --cflags gtk+-2.0)

GTK_LIBS=$(shell pkg-config --libs gthread-2.0) \
		$(shell pkg-config --libs glib-2.0) \
		$(shell pkg-config --libs gtk+-2.0)


ifdef V
JAVAH:=$(JAVAH) -verbose
endif

build/headers: build/headers-static build/headers-generate
	touch $@

build/headers-static: $(HEADERS_C)
	touch $@

tmp/include/%.h: src/jni/%.h
	@echo "CP        $< -> $(@D)"
	cp -p $< $@

# We don't use an implict rule for this for the simple reason that we only
# want to do one invocation, which means using $? (newer than target). It gets
# more complicated because of the need to give classnames to javah.

SOURCES_JNI=$(shell find src/java -name '*.c') $(shell find mockup/java -name '*.c')
build/headers-generate: $(SOURCES_JNI)
	@echo "$(JAVAH_CMD) tmp/headers/*.h"
	$(JAVAH) -jni -d tmp/include -classpath $(JAVAGNOME_JARS):tmp/classes \
		$(shell echo $? | sed -e 's/src\/java\///g' -e 's/mockup\/java\///g' -e 's/\.c//g' -e 's/\//\./g' )
	touch $@

tmp/objects/%.o: src/jni/%.c
	@if [ ! -d $(@D) ] ; then echo "MKDIR     $(@D)" ; mkdir -p $(@D) ; fi
	echo "$(CC_CMD) $@"
	$(CCACHE) $(CC) $(GTK_CFLAGS) -Itmp/include -o $@ -c $<


tmp/objects/%.o: src/java/%.c
	@if [ ! -d $(@D) ] ; then echo "MKDIR     $(@D)" ; mkdir -p $(@D) ; fi
	@echo "$(CC_CMD) $@"
	$(CCACHE) $(CC) $(GTK_CFLAGS) -Itmp/include -o $@ -c $<

tmp/objects/%.o: mockup/java/%.c
	@if [ ! -d $(@D) ] ; then echo "MKDIR     $(@D)" ; mkdir -p $(@D) ; fi
	@echo "$(CC_CMD) $@"
	$(CCACHE) $(CC) $(GTK_CFLAGS) -Itmp/include -o $@ -c $<

tmp/libgtkjni-$(APIVERSION).so: build/config build/headers $(OBJECTS_GLUE) $(OBJECTS_C)
	@echo "$(LINK_CMD) $@"
	$(LINK) -shared \
		 $(GTK_LIBS) \
		-o $@ $(OBJECTS_GLUE) $(OBJECTS_C)
#	@echo "STRIP     $@"
#	strip --only-keep-debug $@

.SECONDARY: tmp/native/gtk.o

tmp/native/gtk.o: tmp/gtk-$(APIVERSION).jar
	@echo "$(GCJ_CMD) $@"
	$(GCJ) -fPIC -fjni \
		-classpath $(JAVAGNOME_JARS):src/java:tmp/classes \
		-o $@ -c $<

tmp/libgtkjava-$(APIVERSION).so: tmp/native/gtk.o
	@echo "$(GCJ_LINK_CMD) $@"
	$(GCJ) -shared -fPIC -fjni \
		-Wl,-rpath=$(JAVAGNOME_HOME)/lib \
		-L$(JAVAGNOME_HOME)/lib \
		-o $@ $<
#	@echo "STRIP     $@"
#	strip --only-keep-debug $@

# WARNING.
# This isn't a complete make install for libgtk-java. It just updates the .jar
# and the .so in an existing installation.

ifdef GCJ
#install: install-java install-native
else
#install: install-java
endif

install-java: build-java \
	$(JAVAGNOME_HOME)/share/java/gtk-$(APIVERSION).jar \
	$(JAVAGNOME_HOME)/lib/libgtkjni-$(APIVERSION).so

install-native: build-native install-java \
	$(JAVAGNOME_HOME)/lib/libgtkjava-$(APIVERSION).so

$(JAVAGNOME_HOME)/share/java/gtk-$(APIVERSION).jar: tmp/gtk-$(APIVERSION).jar
	@echo "CP        $< -> $(@D)"
	cp $< $@
	
$(JAVAGNOME_HOME)/lib/libgtkjni-$(APIVERSION).so: tmp/libgtkjni-$(APIVERSION).so
	@echo "CP        $< -> $(@D)"
	cp $< $@

$(JAVAGNOME_HOME)/lib/libgtkjava-$(APIVERSION).so: tmp/libgtkjava-$(APIVERSION).so
	@echo "CP        $< -> $(@D)"
	cp $< $@

clean:
	@echo "RM        temporary files"
	rm -rf build/* tmp/classes/* tmp/include/* tmp/native/* tmp/objects/*
	rm -f hs_err_*
	@echo "RM        built .jar and .so"
	rm -f tmp/gtk-$(APIVERSION).jar \
		tmp/libgtkjni-$(APIVERSION).so \
		tmp/libgtkjava-$(APIVERSION).so

distclean: clean
	@echo "RM        build configuration information"
	-rm -f .config .config.tmp
	@echo "RM        generated documentation"
	-rm -rf doc/api/*
	-rm -f java-gnome-*.tar.bz2
	@echo "RM        temporary directories"
	-rm -rf tmp build

ifdef V
else
JAVADOC:=$(JAVADOC) -quiet
REDIRECT=>/dev/null
endif

doc: build/classes
	@echo "$(JAVADOC_CMD) doc/api/*.html"
	$(JAVADOC) \
		-d doc/api \
		-classpath src/java \
		-public \
		-source 1.4 \
		-notree \
		-noindex \
		-nohelp \
		-version \
		-author \
		$(SOURCES_JAVA) $(REDIRECT)


#
# Remember that if you bump the version number you need to commit the change
# and re-./configure before being able to run this! On the other hand, we
# don't have to distclean before calling this.
#
dist: all
	@echo "CHECK     fully committed state"
	bzr diff > /dev/null || ( echo -e "\nYou need to commit all changes before running make dist\n" ; exit 4 )
	@echo "EXPORT    java-gnome-$(VERSION).tar.bz2"
	bzr export java-gnome-$(VERSION).tar.bz2


# vim: set filetype=make textwidth=78 nowrap:
