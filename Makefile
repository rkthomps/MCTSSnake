
JC = javac

#JC_FLAGS = -Xlint:unchecked
JC_FLAGS = -g

OUT_FLAG = -d
CP_FLAG = -cp

SRC_DIR = ./src/
TEST_DIR = ./test/
TARGET_DIR = ./target/
PACKAGES_DIR = ./packages/

SRC_LIST=./sources.txt
PACKAGE_LIST=./packages.txt

MAIN_CLASS = $(SRC_DIR)snakes/SnakesUIMain.java

default: main images

main: sources pkgs target
	$(JC) $(JC_FLAGS) $(CP_FLAG) $$(cat $(PACKAGE_LIST)) $(OUT_FLAG) $(TARGET_DIR) @$(SRC_LIST)

sources:
	find $(SRC_DIR) -name "*.java" > $(SRC_LIST)

pkgs:
	PKGS=$$(find packages/*); echo $${PKGS//[[:space:]]/:} > $(PACKAGE_LIST);

target:
	mkdir -p $(TARGET_DIR)

images:
	cp -r $(SRC_DIR)snakes/images $(TARGET_DIR)snakes/images

clean: clean_src clean_ss clean_sources
	$(RM) -r $(TARGET_DIR)

clean_sources:
	$(RM) $(SRC_LIST) $(PACKAGE_LIST)

clean_src:
	find . -type f -name "*~" -delete

clean_ss:
	$(RM) -r ssc_work
