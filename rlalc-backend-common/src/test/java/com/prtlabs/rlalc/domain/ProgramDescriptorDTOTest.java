package com.prtlabs.rlalc.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prtlabs.utils.json.PrtJsonUtils;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProgramDescriptorDTOTest {

    @Test
    public void testSerializeDeserializeWithRecorderSpecificParameters() throws Exception {
        // Create a map of recorder-specific parameters
        Map<String, String> recorderParams = new HashMap<>();
        recorderParams.put("param1", "value1");
        recorderParams.put("param2", "value2");
        
        // Create a ProgramDescriptorDTO with the parameters
        ProgramId programId = new ProgramId(UUID.randomUUID().toString());
        ProgramDescriptorDTO dto = new ProgramDescriptorDTO(
            programId,
            "Test Program",
            "http://example.com/stream",
            System.currentTimeMillis() / 1000,
            3600,
            ZoneId.systemDefault(),
            recorderParams
        );
        
        // Serialize to JSON
        ObjectMapper mapper = PrtJsonUtils.getFasterXmlObjectMapper();
        String json = mapper.writeValueAsString(dto);
        
        System.out.println("[DEBUG_LOG] Serialized JSON: " + json);
        
        // Deserialize from JSON
        ProgramDescriptorDTO deserializedDto = mapper.readValue(json, ProgramDescriptorDTO.class);
        
        // Verify the deserialized object
        assertNotNull(deserializedDto);
        assertEquals(programId, deserializedDto.getUuid());
        assertEquals("Test Program", deserializedDto.getTitle());
        assertEquals("http://example.com/stream", deserializedDto.getStreamURL());
        assertEquals(3600, deserializedDto.getDurationSeconds());
        
        // Verify the recorder-specific parameters
        Map<String, String> deserializedParams = deserializedDto.getRecorderSpecificParameters();
        assertNotNull(deserializedParams);
        assertEquals(2, deserializedParams.size());
        assertEquals("value1", deserializedParams.get("param1"));
        assertEquals("value2", deserializedParams.get("param2"));
    }
}