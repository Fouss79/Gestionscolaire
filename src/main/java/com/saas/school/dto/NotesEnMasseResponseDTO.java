package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotesEnMasseResponseDTO {

    private int nombreNotes;
    private List<NoteResponseDTO> notes;
}
