
package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.NotesPrimaireDtos.DonneesPrimaireDto;
import com.saas.school.dto.NotesPrimaireDtos.ElevePrimaireDto;
import com.saas.school.dto.NotesPrimaireDtos.MatierePrimaireDto;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Classe;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReleveNotesPrimairePdfService {

    private final NotesPrimaireService notesPrimaireService;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    private static final int MATIERES_PAR_PAGE = 5;

    private static final Font TITRE =
            new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);

    private static final Font TEXTE =
            new Font(Font.FontFamily.HELVETICA, 10);

    private static final Font ENTETE =
            new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

    private static final Font PETIT =
            new Font(Font.FontFamily.HELVETICA, 8);


    public byte[] generer(
            Long classeId,
            Long anneeId,
            String mois
    ) {

        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Classe introuvable : " + classeId
                        )
                );

        AnneeScolaire annee = anneeScolaireRepository.findById(anneeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Année scolaire introuvable : " + anneeId
                        )
                );

        String nomClasse = classe.getNomComplet();
        String nomAnnee = annee.getNom();
        DonneesPrimaireDto donnees =
                notesPrimaireService.charger(classeId, anneeId, mois);

        List<ElevePrimaireDto> eleves = donnees.eleves();
        List<MatierePrimaireDto> matieres = donnees.matieres();

        if (eleves == null || eleves.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun élève validé dans cette classe."
            );
        }

        if (matieres == null || matieres.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucune matière trouvée pour cette classe."
            );
        }

        ByteArrayOutputStream sortie = new ByteArrayOutputStream();

        Document document = new Document(
                PageSize.A4.rotate(),
                28,
                28,
                32,
                35
        );

        try {
            PdfWriter.getInstance(document, sortie);
            document.open();

            int nombreGroupes =
                    (matieres.size() + MATIERES_PAR_PAGE - 1)
                            / MATIERES_PAR_PAGE;

            for (int groupe = 0; groupe < nombreGroupes; groupe++) {
                if (groupe > 0) {
                    document.newPage();
                }

                int debut = groupe * MATIERES_PAR_PAGE;
                int fin = Math.min(
                        debut + MATIERES_PAR_PAGE,
                        matieres.size()
                );

                List<MatierePrimaireDto> groupeMatieres =
                        matieres.subList(debut, fin);

                ajouterEntete(
                        document,
                        nomClasse,
                        nomAnnee,
                        mois,
                        groupe + 1,
                        nombreGroupes
                );

                ajouterTableau(
                        document,
                        eleves,
                        groupeMatieres
                );

                ajouterSignature(document);
            }

            document.close();
            return sortie.toByteArray();

        } catch (DocumentException e) {
            throw new IllegalStateException(
                    "Impossible de générer la fiche vierge primaire.",
                    e
            );
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void ajouterEntete(
            Document document,
            String nomClasse,
            String nomAnnee,
            String mois,
            int page,
            int totalPages
    ) throws DocumentException {

        Paragraph titre = new Paragraph(
                "FICHE VIERGE DE RELEVE DE NOTES - PRIMAIRE",
                TITRE
        );

        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(12);
        document.add(titre);

        PdfPTable infos = new PdfPTable(2);
        infos.setWidthPercentage(100);
        infos.setWidths(new float[]{1, 1});
        infos.setSpacingAfter(12);

        infos.addCell(celluleInfo(
                "Classe : " + nomClasse
        ));

        infos.addCell(celluleInfo(
                "Année scolaire : " + nomAnnee
        ));

        infos.addCell(celluleInfo(
                "Mois : " + mois
        ));

        infos.addCell(celluleInfo(
                "Feuille : " + page + " / " + totalPages
        ));

        document.add(infos);

        Paragraph enseignant = new Paragraph(
                "Enseignant(e) : "
                        + "________________________________________",
                TEXTE
        );

        enseignant.setSpacingAfter(12);
        document.add(enseignant);
    }
    private void ajouterTableau(
            Document document,
            List<ElevePrimaireDto> eleves,
            List<MatierePrimaireDto> matieres
    ) throws DocumentException {

        int colonnes = matieres.size() + 2;

        PdfPTable tableau = new PdfPTable(colonnes);
        tableau.setWidthPercentage(100);
        tableau.setHeaderRows(1);
        tableau.setSplitRows(true);
        tableau.setSplitLate(false);

        float[] largeurs = new float[colonnes];
        largeurs[0] = 0.6f;
        largeurs[1] = 3.2f;

        for (int i = 2; i < colonnes; i++) {
            largeurs[i] = 1.5f;
        }

        tableau.setWidths(largeurs);

        tableau.addCell(celluleEntete("N°"));
        tableau.addCell(celluleEntete("NOM ET PRENOM"));

        for (MatierePrimaireDto matiere : matieres) {
            tableau.addCell(
                    celluleEntete(
                            matiere.matiereNom() + "\n/10"
                    )
            );
        }

        int numero = 1;

        for (ElevePrimaireDto eleve : eleves) {
            tableau.addCell(
                    celluleTexte(String.valueOf(numero++))
            );

            tableau.addCell(
                    celluleTexte(
                            valeur(eleve.nom())
                                    + " "
                                    + valeur(eleve.prenom())
                    )
            );

            for (int i = 0; i < matieres.size(); i++) {
                tableau.addCell(celluleVide());
            }
        }

        document.add(tableau);
    }

    private void ajouterSignature(
            Document document
    ) throws DocumentException {

        Paragraph signature = new Paragraph(
                "\nDate : ____________________"
                        + "                       "
                        + "Signature de l'enseignant(e) : "
                        + "________________________",
                TEXTE
        );

        signature.setSpacingBefore(12);
        document.add(signature);
    }

    private PdfPCell celluleEntete(String valeur) {
        PdfPCell cellule = new PdfPCell(
                new Phrase(valeur, ENTETE)
        );

        cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellule.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellule.setMinimumHeight(38);
        cellule.setPadding(5);
        cellule.setBackgroundColor(
                new BaseColor(235, 235, 235)
        );

        return cellule;
    }

    private PdfPCell celluleTexte(String valeur) {
        PdfPCell cellule = new PdfPCell(
                new Phrase(valeur, PETIT)
        );

        cellule.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellule.setMinimumHeight(28);
        cellule.setPadding(5);

        return cellule;
    }

    private PdfPCell celluleVide() {
        PdfPCell cellule = new PdfPCell(
                new Phrase(" ")
        );

        cellule.setMinimumHeight(28);
        cellule.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellule.setVerticalAlignment(Element.ALIGN_MIDDLE);

        return cellule;
    }

    private PdfPCell celluleInfo(String valeur) {
        PdfPCell cellule = new PdfPCell(
                new Phrase(valeur, TEXTE)
        );

        cellule.setBorder(Rectangle.NO_BORDER);
        cellule.setPaddingBottom(5);

        return cellule;
    }

    private String valeur(String texte) {
        return texte == null ? "" : texte;
    }
}