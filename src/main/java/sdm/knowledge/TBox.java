package sdm.knowledge;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.Arrays;
import java.util.List;

public class TBox {

    public static String NS = "KG#";

    public static void main(String[] args) {
        OntModel m = ModelFactory.createOntologyModel();

        OntClass academic = m.createClass(NS + "Academic");

        // Paper classes
        OntClass paper = m.createClass(NS + "Paper");
        OntClass poster = m.createClass(NS + "Poster");
        List<OntClass> paperSubclasses = Arrays.asList(
                m.createClass(NS + "Full paper"),
                m.createClass(NS + "Short paper"),
                m.createClass(NS + "Demo paper"),
                poster
        );
        createPaperClassRelationships(paper, paperSubclasses);

        // Venue classes
        OntClass venue = m.createClass(NS + "Venue");
        OntClass conference = m.createClass(NS + "Conference");
        OntClass journal = m.createClass(NS + "Journal");

        venue.addSubClass(conference);
        venue.addSubClass(journal);

        List<OntClass> conferenceSubclasses = Arrays.asList(
                m.createClass(NS + "Workshop"),
                m.createClass(NS + "Symposium"),
                m.createClass(NS + "Expert Group")
        );

        conferenceSubclasses.forEach(conference::addSubClass);
        makeClassesDisjoint(conferenceSubclasses);

        OntClass proceedings = m.createClass(NS + "Proceedings");
        OntClass volume = m.createClass(NS + "Volume");


        m.listOntProperties().forEach(System.out::println);
    }

    private static void createPaperClassRelationships(OntClass paper, List<OntClass> paperSubclasses) {
        paperSubclasses.forEach(paper::addSubClass);
        makeClassesDisjoint(paperSubclasses);
    }

    private static void makeClassesDisjoint(List<OntClass> disjointClasses) {
        for (int i = 0; i < disjointClasses.size(); i++) {
            OntClass currentClass = disjointClasses.get(i);
            for (int j = i + 1; j < disjointClasses.size(); j++) {
                currentClass.addDisjointWith(disjointClasses.get(j));
            }
        }
    }

}
