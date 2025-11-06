FROM public.ecr.aws/amazoncorretto/amazoncorretto:25

ARG JAR_FILE=appointment-booking-APPOINTMENT-BOOKING-UNSET-VERSION.jar
ENV JAR_FILE=$JAR_FILE

WORKDIR /app

COPY  /build/libs/$JAR_FILE /app/

USER 100
EXPOSE 8072
ENV TZ="Africa/Johannesburg"

ENV JAVA_OPS="-Xms768m -Xmx768m -XX:=ExitOutOfMemoryError -XX:+EnableDynamicAgentLoading -Dreactor.netty.http.server.accessLogEnabled=false"

ENTRYPOINT exec java $JAVA_OPTS -jar $JAR_FILE