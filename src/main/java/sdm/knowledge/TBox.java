package sdm.knowledge;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDFS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TBox {

    public static class Classes {
        public static final String academic = NS + "Academic";
        public static final String author = NS + "Author";
        public static final String reviewer = NS + "Reviewer";
        public static final String handler = NS + "Handler";
        public static final String chair = NS + "Chair";
        public static final String editor = NS + "Editor";
        public static final String paper = NS + "Paper";
        public static final String fullPaper = NS + "Full_paper";
        public static final String shortPaper = NS + "Short_paper";
        public static final String demoPaper = NS + "Demo_paper";
        public static final String poster = NS + "Poster";
        public static final String submission = NS + "Submission";
        public static final String venue = NS + "Venue";
        public static final String conference = NS + "Conference";
        public static final String journal = NS + "Journal";
        public static final String regularConference = NS + "Regular_conference";
        public static final String workshop = NS + "Workshop";
        public static final String symposium = NS + "Symposium";
        public static final String expertGroup = NS + "Expert_group";
        public static final String venuePublication = NS + "Venue_publication";
        public static final String proceedings = NS + "Proceedings";
        public static final String volume = NS + "Volume";
        public static final String revision = NS + "Revision";
        public static final String field = NS + "Field";

        static void makeDisjointCompleteSubclasses(OntModel m, OntClass superClass, List<OntClass> subClasses) {
            makeCompleteSubclasses(m, superClass, subClasses);
            makeClassesDisjoint(subClasses);
        }

        static void makeCompleteSubclasses(OntModel m, OntClass superClass, List<OntClass> subClasses) {
            subClasses.forEach(superClass::addSubClass);
            superClass.addEquivalentClass(m.createUnionClass(internal(), m.createList(subClasses.iterator())));
        }

        static void makeSubclasses(OntClass superClass, OntClass... subClasses) {
            Arrays.stream(subClasses).forEach(superClass::addSubClass);
        }

        static void makeClassesDisjoint(List<OntClass> disjointClasses) {
            for (OntClass left : disjointClasses) {
                for (OntClass right : disjointClasses) {
                    if (left.equals(right)) continue;
                    left.addDisjointWith(right);
                }
            }
        }
    }

    public static class ObjectProperties {
        public static final String authors = NS + "authors";
        public static final String authoredBy = NS + "authored_by";
        public static final String cites = NS + "cites";
        public static final String citedBy = NS + "cited_by";
        public static final String submittedAs = NS + "submitted_as";
        public static final String submittedTo = NS + "submitted_to";
        public static final String publishedIn = NS + "published_in";
        public static final String belongsTo = NS + "belongs_to";
        public static final String publishes = NS + "publishes";
        public static final String manages = NS + "manages";
        public static final String managedBy = NS + "managed_by";
        public static final String assigns = NS + "assigns";
        public static final String doneBy = NS + "done_by";
        public static final String takesPartIn = NS + "takes_part_in";
        public static final String reviews = NS + "reviews";
        public static final String reviewedBy = NS + "reviewed_by";
        public static final String paperRelatedTo = NS + "paper_related_to";
        public static final String venueRelatedTo = NS + "venue_related_to";

        static OntProperty createProperty(OntModel m, String name, OntClass domain, OntClass range) {
            return createProperty(m, name, domain, range, null);
        }

        static OntProperty createProperty(OntModel m, String name, OntClass domain, OntClass range, String inverse) {
            OntProperty property = m.createObjectProperty(name);
            property.setDomain(domain);
            property.setRange(range);
            if (inverse != null) {
                OntProperty inverseProperty = m.createObjectProperty(inverse);
                inverseProperty.setInverseOf(property);
                inverseProperty.setDomain(range);
                inverseProperty.setRange(domain);
            }
            return property;
        }
    }

    public static class DataProperties {
        public static final String name = NS + "name";
        public static final String title = NS + "title";
        public static final String paperAbstract = NS + "abstract";
        public static final String accepted = NS + "accepted";
        public static final String reviewText = NS + "review_text";
        public static final String year = NS + "year";
        public static final String venueName = NS + "venue_name";

        static DatatypeProperty createStringAttribute(OntModel m, String name, OntClass domain) {
            return createAttribute(m, name, domain, XSDDatatype.XSDstring, false);
        }

        static DatatypeProperty createStringAttribute(OntModel m, String name, OntClass domain, boolean single) {
            return createAttribute(m, name, domain, XSDDatatype.XSDstring, single);
        }

        static DatatypeProperty createBooleanAttribute(OntModel m, String name, OntClass domain) {
            return createAttribute(m, name, domain, XSDDatatype.XSDboolean, false);
        }

        static DatatypeProperty createBooleanAttribute(OntModel m, String name, OntClass domain, boolean single) {
            return createAttribute(m, name, domain, XSDDatatype.XSDboolean, single);
        }

        static DatatypeProperty createAttribute(OntModel m, String name, OntClass domain, XSDDatatype range) {
            return createAttribute(m, name, domain, range, false);
        }

        static DatatypeProperty createAttribute(OntModel m, String name, OntClass domain, XSDDatatype range, boolean single) {
            DatatypeProperty property = m.createDatatypeProperty(name);
            property.setDomain(domain);
            property.setRange(m.createResource(range.getURI()));
            if (single) {
                domain.addSuperClass(m.createCardinalityRestriction(internal(), property, 1));
            }
            return property;
        }
    }

    private static class Restrictions {
        static void createCardinalityRestriction(OntModel m, OntClass domain, OntProperty property, int cardinality) {
            domain.addSuperClass(m.createCardinalityRestriction(internal(), property, cardinality));
        }

        static void createMaxCardinalityRestriction(OntModel m, OntClass domain, OntProperty property, int cardinality) {
            domain.addSuperClass(m.createMaxCardinalityRestriction(internal(), property, cardinality));
        }

        static void createMinCardinalityRestriction(OntModel m, OntClass domain, OntProperty property, int cardinality) {
            domain.addSuperClass(m.createMinCardinalityRestriction(internal(), property, cardinality));
        }

        static void createPropertyRestriction(OntModel m, OntProperty property, OntClass subdomain, OntClass subRange) {
            subdomain.addSuperClass(m.createAllValuesFromRestriction(internal(), property, subRange));
        }
    }

    public static String NS = "https://ferrazzi.divi/#";
    private static int count = 1;

    private static String internal() {
        return NS + "Internal" + count++;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Output path is required");
        }
        String outputPath = args[0];

        OntModel m = createBaseModel();
//        OntModel m = createOverkillModel();

        FileOutputStream output = new FileOutputStream(outputPath);
        RDFDataMgr.write(output, m, RDFFormat.TURTLE);
    }

    private static OntModel createBaseModel() {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        m.setNsPrefix("fd", NS);

        // -------------------
        // ------Classes------
        // -------------------

        // People classes

        OntClass academic = m.createClass(Classes.academic);

        OntClass author = m.createClass(Classes.author);
        OntClass reviewer = m.createClass(Classes.reviewer);
        OntClass handler = m.createClass(Classes.handler);
        Classes.makeSubclasses(academic, author, reviewer, handler);

        OntClass chair = m.createClass(Classes.chair);
        OntClass editor = m.createClass(Classes.editor);
        Classes.makeSubclasses(handler, chair, editor);


        // Paper classes
        OntClass paper = m.createClass(Classes.paper);
        OntClass fullPaper = m.createClass(Classes.fullPaper);
        OntClass shortPaper = m.createClass(Classes.shortPaper);
        OntClass demoPaper = m.createClass(Classes.demoPaper);
        OntClass poster = m.createClass(Classes.poster);
        Classes.makeSubclasses(paper, fullPaper, shortPaper, demoPaper, poster);

        OntClass submission = m.createClass(Classes.submission);

        // Venue classes
        OntClass venue = m.createClass(Classes.venue);

        OntClass conference = m.createClass(Classes.conference);
        OntClass journal = m.createClass(Classes.journal);
        Classes.makeSubclasses(venue, conference, journal);

        OntClass regularConference = m.createClass(Classes.regularConference);
        OntClass workshop = m.createClass(Classes.workshop);
        OntClass symposium = m.createClass(Classes.symposium);
        OntClass expertGroup = m.createClass(Classes.expertGroup);
        Classes.makeSubclasses(conference, regularConference, workshop, symposium, expertGroup);

        // Venue Publication
        OntClass venuePublication = m.createClass(Classes.venuePublication);

        OntClass proceedings = m.createClass(Classes.proceedings);
        OntClass volume = m.createClass(Classes.volume);
        Classes.makeSubclasses(venuePublication, proceedings, volume);

        // Revision
        OntClass revision = m.createClass(Classes.revision);

        // Field
        OntClass field = m.createClass(Classes.field);

        // ------------------
        // ----Properties----
        // ------------------

        ObjectProperties.createProperty(m, ObjectProperties.authors, author, paper, ObjectProperties.authoredBy);

        ObjectProperties.createProperty(m, ObjectProperties.cites, paper, paper, ObjectProperties.citedBy);
        ObjectProperties.createProperty(m, ObjectProperties.submittedAs, paper, submission);
        ObjectProperties.createProperty(m, ObjectProperties.submittedTo, submission, venue);
        ObjectProperties.createProperty(m, ObjectProperties.publishedIn, submission, venuePublication);

        ObjectProperties.createProperty(m, ObjectProperties.belongsTo, venuePublication, venue, ObjectProperties.publishes);

        ObjectProperties.createProperty(m, ObjectProperties.manages, handler, venue, ObjectProperties.managedBy);

        ObjectProperties.createProperty(m, ObjectProperties.assigns, handler, revision);
        ObjectProperties.createProperty(m, ObjectProperties.doneBy, revision, reviewer, ObjectProperties.takesPartIn);
        ObjectProperties.createProperty(m, ObjectProperties.reviews, revision, submission, ObjectProperties.reviewedBy);

        ObjectProperties.createProperty(m, ObjectProperties.paperRelatedTo, paper, field);
        ObjectProperties.createProperty(m, ObjectProperties.venueRelatedTo, venue, field);

        // --------------------
        // -----Attributes-----
        // --------------------

        // People attributes
        DatatypeProperty name = DataProperties.createStringAttribute(m, DataProperties.name, academic);
        name.addSuperProperty(RDFS.label);

        // Paper attributes
        DatatypeProperty paperTitle = DataProperties.createStringAttribute(m, DataProperties.title, paper);
        paperTitle.addSuperProperty(RDFS.label);
        DatatypeProperty paperAbstract = DataProperties.createStringAttribute(m, DataProperties.paperAbstract, paper);
        paperAbstract.addSuperProperty(RDFS.comment);

        // Revision attributes
        DataProperties.createBooleanAttribute(m, DataProperties.accepted, revision);
        DatatypeProperty revisionText = DataProperties.createBooleanAttribute(m, DataProperties.reviewText, revision);
        revisionText.addSuperProperty(RDFS.comment);

        // Field attributes

        // Venue attributes
        DatatypeProperty venueName = DataProperties.createStringAttribute(m, DataProperties.venueName, venue);
        venueName.addSuperProperty(RDFS.label);

        // Venue publication attributes
        DataProperties.createAttribute(m, DataProperties.year, venuePublication, XSDDatatype.XSDgYear);


        return m;
    }

    private static OntModel createOverkillModel() {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        m = ModelFactory.createOntologyModel();
        m.setNsPrefix("fd", NS);

        // -------------------
        // ------Classes------
        // -------------------

        // People classes

        OntClass academic = m.createClass(NS + "Academic");

        OntClass author = m.createClass(NS + "Author");
        OntClass reviewer = m.createClass(NS + "Reviewer");
        OntClass handler = m.createClass(NS + "Handler");
        Classes.makeCompleteSubclasses(m, academic, Arrays.asList(author, reviewer, handler));

        OntClass chair = m.createClass(NS + "Chair");
        OntClass editor = m.createClass(NS + "Editor");
        Classes.makeCompleteSubclasses(m, handler, Arrays.asList(chair, editor));

        // Paper classes
        OntClass paper = m.createClass(NS + "Paper");
        List<OntClass> paperSubclasses = Arrays.asList(m.createClass(NS + "Full_paper"), m.createClass(NS + "Short_paper"), m.createClass(NS + "Demo_paper"), m.createClass(NS + "Poster"));
        Classes.makeDisjointCompleteSubclasses(m, paper, paperSubclasses);

        // Venue classes
        OntClass venue = m.createClass(NS + "Venue");

        OntClass conference = m.createClass(NS + "Conference");
        OntClass journal = m.createClass(NS + "Journal");
        Classes.makeDisjointCompleteSubclasses(m, venue, Arrays.asList(conference, journal));

        List<OntClass> conferenceSubclasses = Arrays.asList(m.createClass(NS + "Regular_conference"), m.createClass(NS + "Workshop"), m.createClass(NS + "Symposium"), m.createClass(NS + "Expert_group"));
        Classes.makeDisjointCompleteSubclasses(m, conference, conferenceSubclasses);

        // Venue Publication
        OntClass venuePublication = m.createClass(NS + "Venue_publication");

        OntClass proceedings = m.createClass(NS + "Proceedings");
        OntClass volume = m.createClass(NS + "Volume");
        Classes.makeDisjointCompleteSubclasses(m, venuePublication, Arrays.asList(proceedings, volume));

        // Revision
        OntClass revision = m.createClass(NS + "Revision");

        // Field
        OntClass field = m.createClass(NS + "Field");

        Classes.makeClassesDisjoint(Arrays.asList(academic, venue, venuePublication, field, revision));

        // ------------------
        // ----Properties----
        // ------------------

        OntProperty authors = ObjectProperties.createProperty(m, NS + "authors", author, paper, NS + "authored_by");
        m.createMinCardinalityRestriction(internal(), authors, 1);
        m.createMinCardinalityRestriction(internal(), authors.getInverse(), 1);

        OntProperty belongsTo = ObjectProperties.createProperty(m, NS + "belongs_to", venuePublication, venue, NS + "publishes");
        m.createCardinalityRestriction(internal(), belongsTo, 1);
        Restrictions.createPropertyRestriction(m, belongsTo, proceedings, conference);
        Restrictions.createPropertyRestriction(m, belongsTo, volume, journal);

        OntProperty manages = ObjectProperties.createProperty(m, NS + "manages", handler, venue, NS + "managed_by");
        Restrictions.createMinCardinalityRestriction(m, handler, manages, 1);
        Restrictions.createMinCardinalityRestriction(m, venue, manages.getInverse(), 1);
        Restrictions.createPropertyRestriction(m, manages, editor, journal);
        Restrictions.createPropertyRestriction(m, manages, chair, conference);

        ObjectProperties.createProperty(m, NS + "assigns", handler, revision);
        OntProperty doneBy = ObjectProperties.createProperty(m, NS + "done_by", revision, reviewer, NS + "takes_part_in");
        m.createMinCardinalityRestriction(internal(), doneBy, 2);
        OntProperty reviews = ObjectProperties.createProperty(m, NS + "reviews", revision, paper, NS + "reviewed_by");
        m.createCardinalityRestriction(internal(), reviews, 1);

        OntProperty relatedTo = ObjectProperties.createProperty(m, NS + "related_to", m.createUnionClass(null, m.createList(paper, venue)), field);
        m.createMinCardinalityRestriction(internal(), relatedTo, 1);

        // --------------------
        // -----Attributes-----
        // --------------------

        // Name attribute
//        DatatypeProperty name = createStringAttribute(m, NS + "name",
//                m.createUnionClass(internal(), m.createList(academic, field, venue)), true);
//        name.addSuperProperty(RDFS.label);

        // People attributes
        DatatypeProperty name = DataProperties.createStringAttribute(m, NS + "name", academic, true);
        name.addSuperProperty(RDFS.label);


        // Paper attributes
        DatatypeProperty paperTitle = DataProperties.createStringAttribute(m, NS + "title", paper, true);
        paperTitle.addSuperProperty(RDFS.label);
        DatatypeProperty paperAbstract = DataProperties.createStringAttribute(m, NS + "abstract", paper, true);
        paperAbstract.addSuperProperty(RDFS.comment);

        // Revision attributes
        DataProperties.createBooleanAttribute(m, NS + "accepted", revision, true);
        DatatypeProperty revisionText = DataProperties.createBooleanAttribute(m, NS + "review_text", revision, true);
        revisionText.addSuperProperty(RDFS.comment);

        // Field attributes

        // Venue attributes

        // Venue publication attributes
        DatatypeProperty year = DataProperties.createAttribute(m, NS + "Year", venuePublication, XSDDatatype.XSDgYear, true);
        return m;
    }

}
