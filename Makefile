PROJECT := tap

SRCDIR  := src
DISTDIR := dist
DOCDIR  := doc
NDDIR   := nd

JAVASRC != cd ${SRCDIR} && find * -type f -name \*.java
CLJSRC  != cd ${SRCDIR} && find * -type f -name \*.clj

JAR     := ${PROJECT}.jar
TGZ     != echo ${PROJECT}-`shtool version -d short version.txt`.tar.gz

all: jar

jar: ${JAR}

tarball: ${TGZ}

test: jar
	@echo "--> Running Unit Tests.."
	@env CLASSPATH=${JAR}:$${CLASSPATH} prove t

doc:
	@echo "--> Generating Documentation.."
	@../../NaturalDocs/NaturalDocs -nag -s kotka \
		-i ${SRCDIR} -o HTML ${DOCDIR} -p ${NDDIR}

clean:
	@echo "--> Cleaning Up.."
	@rm -rf ${DISTDIR} ${JAR} *.tar.gz ${DOCDIR}

compile.clj: ${DISTDIR}
	@if [ -n "${CLJSRC}" ]; then \
		echo "--> Compiling Clojure Sources.."; \
		for clj in ${CLJSRC}; do \
			shtool mkdir -p ${DISTDIR}/`dirname $${clj}`; \
			shtool install -c ${SRCDIR}/$${clj} ${DISTDIR}/$${clj}; \
		done; \
	fi

${JAR}: compile.clj
	@echo "--> Creating JAR File.."
	@jar cf ${JAR} -C ${DISTDIR} ${CLJSRC}

${TGZ}:
	@echo "--> Creating Source Tarball.."
	@shtool tarball -c "gzip -9" -o ${TGZ} \
		-e '\.DS_Store,${DISTDIR},${JAR},\.hg,\.tar\.gz' .

${DISTDIR}:
	@echo "--> Creating Build Directory.."
	@shtool mkdir -p ${DISTDIR}

.PHONY: all jar test doc clean compile.clj
