
JC = javac

#JC_FLAGS = -Xlint:unchecked
JC_FLAGS = -g

SRC_FLAG = -sourcepath
OUT_FLAG = -d
CP_FLAG = -cp

SRC_DIR = ./src/
TEST_DIR = ./test/
TARGET_DIR = ./target/

SRC_LIST=./sources.txt

MAIN_CLASS = $(SRC_DIR)snakes/SnakesUIMain.java

default: main images

main: sources
	$(JC) $(JC_FLAGS) $(SRC_FLAG) $(SRC_DIR) $(OUT_FLAG) $(TARGET_DIR) @$(SRC_LIST)

sources:
	find $(SRC_DIR) -name "*.java" > $(SRC_LIST)

images:
	cp -r $(SRC_DIR)snakes/images $(TARGET_DIR)snakes/images

clean: clean_src clean_ss clean_sources
	$(RM) -r $(TARGET_DIR)

clean_sources:
	$(RM) $(SRC_LIST)

clean_src:
	find . -type f -name "*~" -delete

clean_ss:
	$(RM) -r ssc_work
