# Makefile for QuadCopApp
#
# Copyright (C) 2017, 2018 Jürgen Reuter
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

ROOT_DIR=..
SRC_DIR=$(ROOT_DIR)/src
BUILD_DIR=$(ROOT_DIR)/build
JAR_DIR=$(ROOT_DIR)/jar

RXTXCOMM_JAR=/usr/share/java/RXTXcomm.jar
CLASSPATH=$(SRC_DIR):$(BUILD_DIR):$(RXTXCOMM_JAR)
JNI_SO_DIR=/usr/lib/jni

JAVA_SRC=$(wildcard $(SRC_DIR)/org/soundpaint/qcapp/*.java)
JAVA_OBJ=$(patsubst $(SRC_DIR)/%.java,$(BUILD_DIR)/%.class,$(JAVA_SRC))

IMAGES_SRC=$(wildcard $(SRC_DIR)/images/*.png)
IMAGES_OBJ=$(patsubst $(SRC_DIR)/%,$(BUILD_DIR)/%,$(IMAGES_SRC))
JAR_OBJ=$(JAR_DIR)/QuadCopApp.jar

all: obj jar

obj: $(BUILD_DIR) $(JAVA_OBJ) $(IMAGES_OBJ)

$(BUILD_DIR):
	mkdir -p $@

$(BUILD_DIR)/images:
	mkdir -p $@

$(JAR_DIR):
	mkdir -p $@

$(BUILD_DIR)/images/%: $(SRC_DIR)/images/% | $(BUILD_DIR)/images
	cp -fp $< $@

jar: $(JAR_OBJ)

$(JAR_OBJ): $(JAR_DIR) $(JAVA_OBJ)
	cd $(BUILD_DIR); \
	jar -0cvfm $(JAR_DIR)/QuadCopApp.jar $(SRC_DIR)/META-INF/MANIFEST.MF .

depend:

tags:
	etags *.java

run:
	java -Djava.library.path=$(JNI_SO_DIR) -jar $(JAR_OBJ)

objclean:
	- rm -rf $(BUILD_DIR)

jarclean:
	- rm -rf $(JAR_DIR)

bkpclean:
	- find $(SRC_DIR) -name \*~ -exec /bin/rm -f {} \; -print

clean: objclean jarclean

distclean: clean bkpclean

tarball: distclean
	@cd .. ; \
	TGZ_DATE=`date +%Y-%m-%d_%H-%M-%S` ; \
	PROJECT_NAME=QuadCopApp ; \
	PROJECT_PATH=`basename \`pwd\`` ; \
	TGZ_PREFIX=$$PROJECT_NAME\_$$TGZ_DATE ; cd .. ; \
	tar cvf ./$$TGZ_PREFIX.tar.bz2 \
		--exclude=.git \
		--exclude=untracked_files \
		--transform=s/$$PROJECT_PATH/$$TGZ_PREFIX/ \
		--bzip2 $$PROJECT_PATH

.SUFFIXES: $(SUFFIXES) .java .class

$(BUILD_DIR)%class: $(SRC_DIR)%java
	javac -Xlint:all -d $(BUILD_DIR) -classpath $(CLASSPATH) $<

#  Local Variables:
#    coding:utf-8
#    mode:Makefile
#  End:
