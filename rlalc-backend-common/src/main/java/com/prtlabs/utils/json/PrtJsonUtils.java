package com.prtlabs.utils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Utility methods to easy working with JSON and Jackson
 */
public class PrtJsonUtils {

    /**
     *
     * @return
     */
    public static final ObjectMapper getFasterXmlObjectMapper() {
        boolean mapperShouldFailOnUnknownProperties = false;
        boolean mapperShouldFailOnMissingCreatorProperties = true;
        return getFasterXmlObjectMapper(mapperShouldFailOnUnknownProperties, mapperShouldFailOnMissingCreatorProperties);
    }

    /**
     * Help create a Mapper with custom behaviors:
     *
     *   - The 'mapperShouldFailOnUnknownProperties' param is used to control if unknown properties are accepted.
     *
     *   - The 'mapperShouldFailOnMissingCreatorProperties' param is used to control if some properties are mandatory for
     *     instance, the below method makes the 'filterName' property mandatory
     *
     *        @JsonCreator
     *        public FilteringDTO(@JsonProperty("filterName") String filterName) {
     *           this.filterName = filterName;
     *        }
     *
     * @return
     */
    public static final ObjectMapper getFasterXmlObjectMapper(boolean mapperShouldFailOnUnknownProperties, boolean mapperShouldFailOnMissingCreatorProperties) {
        ObjectMapper mapper = new ObjectMapper();
        if (mapperShouldFailOnUnknownProperties) {
            mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        } else {
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (mapperShouldFailOnMissingCreatorProperties) {
            mapper.enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        } else {
            mapper.disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        }
        mapper.registerModule(new Jdk8Module());	           // This is useful to allow serialization/deserialization of Optional<String> (cf https://www.baeldung.com/jackson-optional#1-maven-dependency-and-registration)
        mapper.registerModule(new JavaTimeModule());           // Needed for the proper deserialization of dates (needed for JSR310)
        mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true );                 // We want dates (Date/Calendar/OffsetDateTime) to be serialized as epoch
        mapper.configure( SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false );     // We want to write epochMillis (and not epochNanos)
        mapper.configure( DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false );    // We want to reade epochMillis (and not epochNanos)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);    // Do not serialize null fields (ie 'Only serialize fields which are not null')
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);  // Do not serialize Optional.empty fields (ie 'Only serialize Optional non-empty fields)
        return mapper;
    }

}
