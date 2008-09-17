PROJECT := tap

SRCDIR  := src
DISTDIR := dist

JAVASRC != cd ${SRCDIR} && find * -type f -name \*.java
CLJSRC  != cd ${SRCDIR} && find * -type f -name \*.clj

JAR     := ${PROJECT}.jar
TGZ     != echo ${PROJECT}-`shtool version -d short version.txt`.tar.gz

all: jar

jar: ${JAR}

tarball: ${TGZ}

test: jar
	@echo "Running Unit Tests.."
	@env CLASSPATH=${JAR}:$${CLASSPATH} prove t

clean:
	@echo "Cleaning.."
	@rm -rf ${DISTDIR} ${JAR} *.tar.gz

compile.clj: ${DISTDIR}
	@if [ -n "${CLJSRC}" ]; then \
		echo "Compiling Clojure Sources.."; \
		for clj in ${CLJSRC}; do \
			shtool mkdir -p ${DISTDIR}/`dirname $${clj}`; \
			shtool install -c ${SRCDIR}/$${clj} ${DISTDIR}/$${clj}; \
		done; \
	fi

${JAR}: compile.clj
	@echo "Creating JAR File.."
	@jar cf ${JAR} -C ${DISTDIR} ${CLJSRC}

${TGZ}:
	@echo "Creating Source Tarball.."
	@shtool tarball -c "gzip -9" -o ${TGZ} \
		-e '\.DS_Store,${DISTDIR},${JAR},\.hg,\.tar\.gz' .

${DISTDIR}:
	@echo "Creating Build Directory.."
	@shtool mkdir -p ${DISTDIR}

.PHONY: all jar test clean compile.clj
