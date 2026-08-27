package com.olamide.receipthandler;

import com.olamide.receipthandler.models.Staff;
import com.olamide.receipthandler.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for the public POST /api/receipts/self-upload endpoint
 * (no auth, no session). Locks in the documented error shapes so a
 * regression like the 403-vs-400 error-dispatch bug can't come back.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SelfUploadContractTests {

    @Autowired
    private StaffRepository staffRepository;

    @LocalServerPort
    private int port;


    private RestTemplate client() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return template;
    }

    private static final String EMPLOYEE_ID = "TST-CONTRACT-" + System.nanoTime();

    @BeforeEach
    void setUp() {
        // The roster entry the employeeId check validates against.
        staffRepository.save(new Staff("Contract Test", EMPLOYEE_ID));
    }

    @AfterEach
    void tearDown() {
        staffRepository.findByActiveTrueAndEmployeeIdIgnoreCase(EMPLOYEE_ID)
                .ifPresent(staffRepository::delete);
    }

    /** A multipart body with the identity fields but zero file parts. */
    private ResponseEntity<String> postSelfUpload(String employeeId, boolean withFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("firstName", "Contract");
        body.add("lastName", "Test");
        body.add("employeeId", employeeId);
        if (withFile) {
            body.add("files", new ByteArrayResource("not really a receipt".getBytes()) {
                @Override
                public String getFilename() {
                    return "dummy.txt";
                }
            });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return client().exchange(
                "http://localhost:" + port + "/api/receipts/self-upload",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);
    }

    @Test
    void selfUploadWithNoFilesReturns400() {
        ResponseEntity<String> response = postSelfUpload(EMPLOYEE_ID, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_FILE_FORMAT");
        assertThat(response.getBody()).contains("No files were uploaded.");
    }

    @Test
    void selfUploadWithUnknownEmployeeIdReturns404() {
        ResponseEntity<String> response = postSelfUpload("NOPE-0000", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("UNKNOWN_EMPLOYEE_ID");
    }
}
