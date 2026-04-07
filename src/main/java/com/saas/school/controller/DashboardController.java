package com.saas.school.controller;

import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.MatiereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final ClasseRepository classeRepository;

    @GetMapping("/stats/{ecoleId}")
    public Map<String, Object> getStats(@PathVariable Long ecoleId) {

        long enseignants = enseignantRepository.countByEcoleId(ecoleId);
        long matieres = matiereRepository.countByEcoleId(ecoleId);
        long classes = classeRepository.countByEcoleId(ecoleId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("enseignants", enseignants);
        stats.put("matieres", matieres);
        stats.put("classes", classes);

        return stats;
    }

    @GetMapping("/chart/{ecoleId}")
    public List<Map<String, Object>> getChartData(@PathVariable Long ecoleId) {

        long enseignants = enseignantRepository.countByEcoleId(ecoleId);
        long matieres = matiereRepository.countByEcoleId(ecoleId);
        long classes = classeRepository.countByEcoleId(ecoleId);

        List<Map<String, Object>> data = new ArrayList<>();

        data.add(Map.of("name", "Enseignants", "value", enseignants));
        data.add(Map.of("name", "Matières", "value", matieres));
        data.add(Map.of("name", "Classes", "value", classes));

        return data;
    }

}
