package com.ykul.flowManager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "file_records")
@Getter
@Setter
@NoArgsConstructor
public class FileRecord {

    @Id
    private UUID id;

    private String fileName;

    private String sourcePath;

    private String convertedPath;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    private String errorMessage;
}