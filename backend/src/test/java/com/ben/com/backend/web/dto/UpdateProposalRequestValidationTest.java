package com.ben.com.backend.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UpdateProposalRequestValidationTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;
	private final JsonMapper mapper = JsonMapper.builder().build();

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void tearDownValidator() {
		if (validatorFactory != null) {
			validatorFactory.close();
		}
	}

	@Test
	void typicalFrontendPayloadPassesValidation() {
		var json = """
				{
				  "proposalNumber": "P-001",
				  "title": "測試提案",
				  "content": "提案內容",
				  "type": "GENERAL",
				  "startTime": null,
				  "endTime": null,
				  "visible": false,
				  "voteOptions": [
				    {"label": "同意方案A", "description": "方案A內容", "passOption": true},
				    {"label": "反對", "description": null, "passOption": false},
				    {"label": "棄權", "description": null, "passOption": false}
				  ],
				  "passThresholdNumerator": 1,
				  "passThresholdDenominator": 2,
				  "thresholdBase": "ATTENDED",
				  "allowRevote": true
				}
				""";
		var request = mapper.readValue(json, UpdateProposalRequest.class);
		assertThat(violations(request)).isEmpty();
	}

	@Test
	void zeroThresholdFailsValidation() {
		var json = """
				{
				  "proposalNumber": "P-001",
				  "title": "測試提案",
				  "content": "提案內容",
				  "type": "GENERAL",
				  "voteOptions": [{"label": "同意", "passOption": true}],
				  "passThresholdNumerator": 0,
				  "passThresholdDenominator": 2,
				  "thresholdBase": "ATTENDED",
				  "allowRevote": true
				}
				""";
		var request = mapper.readValue(json, UpdateProposalRequest.class);
		assertThat(violations(request)).isNotEmpty();
	}

	@Test
	void blankVoteOptionLabelFailsValidation() {
		var json = """
				{
				  "proposalNumber": "P-001",
				  "title": "測試提案",
				  "content": "提案內容",
				  "type": "GENERAL",
				  "voteOptions": [
				    {"label": "同意", "passOption": true},
				    {"label": "", "passOption": false}
				  ],
				  "passThresholdNumerator": 1,
				  "passThresholdDenominator": 2,
				  "thresholdBase": "ATTENDED",
				  "allowRevote": true
				}
				""";
		var request = mapper.readValue(json, UpdateProposalRequest.class);
		assertThat(violations(request)).isNotEmpty();
	}

	private Set<?> violations(UpdateProposalRequest request) {
		return validator.validate(request);
	}
}
