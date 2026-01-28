FROM wiremock/wiremock:3.5.4

# Copy mappings and response files
COPY responses/mappings /home/wiremock/mappings
COPY responses/__files /home/wiremock/__files

EXPOSE 8080

# Enable response templating and verbose logging
CMD ["--global-response-templating", "--verbose"]
