CLASSPATH=".:../bin:../lib/junit.jar:../lib/trove-3.0.0a3.jar:../lib/mtj-0.9.12.jar:../lib/log4j-1.2.15.jar:../lib/jargs.jar"
java -mx4000m -ea -cp ${CLASSPATH} openie.extractor.Test -c $1 
