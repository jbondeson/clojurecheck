PROJECT := tap

SRCDIR  := src
DISTDIR := dist

JAVASRC != cd ${SRCDIR} && find * -type f -name \*.java
CLJSRC  != cd ${SRCDIR} && find * -type f -name \*.clj
DIRS    != cd ${SRCDIR} && find * -type d

VERSION != shtool version -d short version.txt
JAR     := ${PROJECT}-${VERSION}.jar
TGZ     := ${PROJECT}-${VERSION}.tar.gz

all: jar

release: jar tarball

jar: ${JAR}

tarball: ${TGZ}

test: jar
	env CLASSPATH=${JAR}:$${CLASSPATH} prove t

doc:
	( cat README.txt.in; \
	  java clojure.lang.Script gen-docs.clj ) > README.txt

clean:
	rm -rf ${DISTDIR} ${JAR} ${TGZ} README.txt

compile.clj: ${DISTDIR}
	@for clj in ${CLJSRC}; do \
		echo shtool install -C ${SRCDIR}/$${clj} ${DISTDIR}/$${clj}; \
		shtool install -C ${SRCDIR}/$${clj} ${DISTDIR}/$${clj}; \
	done

${JAR}: doc compile.clj
	cp README.txt ${DISTDIR}
	cp LICENSE ${DISTDIR}
	jar cf ${JAR} -C ${DISTDIR} .

${TGZ}: doc
	shtool tarball -c "gzip -9" -o ${TGZ} \
		-e '\.DS_Store,${DISTDIR},\.jar,\.hg,\.tar\.gz' .

${DISTDIR}:
	shtool mkdir -p ${DISTDIR}
	@for dir in ${DIRS}; do \
		echo shtool mkdir -p ${DISTDIR}/$${dir}; \
		shtool mkdir -p ${DISTDIR}/$${dir}; \
	done

.PHONY: all release jar tarball test doc clean compile.clj
