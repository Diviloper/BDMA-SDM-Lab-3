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

        // People classes
        OntClass academic = m.createClass(NS + "Academic");

        OntClass author = m.createClass(NS + "Author");
        OntClass reviewer = m.createClass(NS + "Reviewer");
        OntClass handler = m.createClass(NS + "Handler");
        makeCompleteSubclasses(m, academic, Arrays.asList(author, reviewer, handler));

        OntClass chair = m.createClass(NS + "Chair");
        OntClass editor = m.createClass(NS + "Editor");
        makeDisjointCompleteSubclasses(m, handler, Arrays.asList(chair, editor));


        // Paper classes
        OntClass paper = m.createClass(NS + "Paper");
        List<OntClass> paperSubclasses = Arrays.asList(
                m.createClass(NS + "Full paper"),
                m.createClass(NS + "Short paper"),
                m.createClass(NS + "Demo paper"),
                m.createClass(NS + "Poster")
        );
        makeDisjointCompleteSubclasses(m, paper, paperSubclasses);

        // Venue classes
        OntClass venue = m.createClass(NS + "Venue");

        OntClass conference = m.createClass(NS + "Conference");
        OntClass journal = m.createClass(NS + "Journal");
        makeDisjointCompleteSubclasses(m, venue, Arrays.asList(conference, journal));

        List<OntClass> conferenceSubclasses = Arrays.asList(
                m.createClass(NS + "Regular Conference"),
                m.createClass(NS + "Workshop"),
                m.createClass(NS + "Symposium"),
                m.createClass(NS + "Expert Group")
        );
        makeDisjointCompleteSubclasses(m, conference, conferenceSubclasses);

        // Venue Publication
        OntClass venuePublication = m.createClass(NS + "Venue Publication");

        OntClass proceedings = m.createClass(NS + "Proceedings");
        OntClass volume = m.createClass(NS + "Volume");
        makeDisjointCompleteSubclasses(m, venuePublication, Arrays.asList(proceedings, volume));

        // Revision
        OntClass revision = m.createClass(NS + "Revision");


        m.listClasses().forEach(System.out::println);
    }

    private static OntClass createSubclass(OntModel m, OntClass superClass, String className) {
        OntClass newClass = m.createClass(NS + className);
        newClass.addSuperClass(superClass);
        return newClass;
    }

    private static void makeDisjointCompleteSubclasses(OntModel m, OntClass superClass, List<OntClass> subClasses) {
        subClasses.forEach(superClass::addSubClass);
        makeCompleteSubclasses(m, superClass, subClasses);
        makeClassesDisjoint(subClasses);
    }

    private static void makeCompleteSubclasses(OntModel m, OntClass superClass, List<OntClass> subClasses) {
        superClass.addEquivalentClass(m.createUnionClass(null, m.createList(subClasses.iterator())));
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
