PROJECT := tap

SRCDIR  := src
DISTDIR := dist

JAVASRC != cd ${SRCDIR} && find * -type f -name \*.java
CLJSRC  != cd ${SRCDIR} && find * -type f \( -name \*.clj -and -not -name \*.gen.clj \)
GCCLJSRC!= cd ${SRCDIR} && find * -type f -name \*.gen.clj
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

doc: compile
	( cat README.txt.in; \
	  java clojure.lang.Script gen-docs.clj ) > README.txt

clean:
	rm -rf ${DISTDIR} ${JAR} ${TGZ} README.txt

compile: ${CLJSRC:C/^/dist\//} ${GCCLJSRC:R:R:C/^/dist\//:C/$/.class/}

bump-version:
	shtool version -l txt -n ${PROJECT} -i v version.txt

bump-revision:
	shtool version -l txt -n ${PROJECT} -i r version.txt

bump-level:
	shtool version -l txt -n ${PROJECT} -i l version.txt

.for _clj in ${CLJSRC}
dist/${_clj}: src/${_clj} ${DISTDIR}
	shtool install -c src/${_clj} dist/${_clj}
.endfor

.for _clj in ${GCCLJSRC}
dist/${_clj:R:R}.class: src/${_clj} ${DISTDIR}
	java clojure.lang.Script gen-class.clj -- ${DISTDIR} ${_clj}
.endfor

${JAR}: doc compile
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

.PHONY: all release jar tarball test doc clean compile
