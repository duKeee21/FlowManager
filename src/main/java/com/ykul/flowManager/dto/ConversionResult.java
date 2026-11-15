package com.ykul.flowManager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversionResult {
    private String originalEventId;
    private List<String> outputFilePaths;
    private String errorMessage;
}