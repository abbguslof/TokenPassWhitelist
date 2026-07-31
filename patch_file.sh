sed -i '458,460c\
        try (OutputStream os = exchange.getResponseBody()) {\
            os.write(bytes);\
        }\
    }' VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java
echo "}" >> VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java
