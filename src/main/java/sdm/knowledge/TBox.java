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
        public static final String location = DBPO + "Location";

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
        public static final String ofPaper = NS + "of";
        public static final String submittedTo = NS + "submitted_to";
        public static final String hasSubmission = NS + "has_submission";
        public static final String publishedIn = NS + "published_in";
        public static final String hasPublication = NS + "has_publication";
        public static final String belongsTo = NS + "belongs_to";
        public static final String publishes = NS + "publishes";
        public static final String manages = NS + "manages";
        public static final String managedBy = NS + "managed_by";
        public static final String assigns = NS + "assigns";
        public static final String assignedBy = NS + "assigned_by";
        public static final String doneBy = NS + "done_by";
        public static final String takesPartIn = NS + "takes_part_in";
        public static final String reviews = NS + "reviews";
        public static final String reviewedBy = NS + "reviewed_by";
        public static final String paperRelatedTo = NS + "paper_related_to";
        public static final String venueRelatedTo = NS + "venue_related_to";
        public static final String takesPlaceIn = NS + "takes_place_in";

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
        public static final String doi = NS + "doi";
        public static final String paperAbstract = NS + "abstract";
        public static final String accepted = NS + "accepted";
        public static final String reviewText = NS + "review_text";
        public static final String revisionDateStart = NS + "revision_date_start";
        public static final String revisionDateEnd = NS + "revision_date_end";
        public static final String submissionDate = NS + "submission_date";
        public static final String submissionAcceptedDate = NS + "submission_accepted_date";
        public static final String year = NS + "year";
        public static final String venueName = NS + "venue_name";
        public static final String keyword = NS + "keyword";

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
    public static String DBPO = "http://dbpedia.org/Ontology/";
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
//        extendModel(m);

        FileOutputStream output = new FileOutputStream(outputPath);
        RDFDataMgr.write(output, m, RDFFormat.TURTLE);
    }

    private static OntModel createBaseModel() {
        OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        m.setNsPrefix("fd", NS);
        m.setNsPrefix("dbpo", DBPO);

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
        OntClass location = m.createClass(Classes.location);

        // Revision
        OntClass revision = m.createClass(Classes.revision);

        // Field
        OntClass field = m.createClass(Classes.field);

        // ------------------
        // ----Properties----
        // ------------------

        ObjectProperties.createProperty(m, ObjectProperties.authors, author, paper, ObjectProperties.authoredBy);

        ObjectProperties.createProperty(m, ObjectProperties.cites, paper, paper, ObjectProperties.citedBy);
        ObjectProperties.createProperty(m, ObjectProperties.submittedAs, paper, submission, ObjectProperties.ofPaper);
        ObjectProperties.createProperty(m, ObjectProperties.submittedTo, submission, venue, ObjectProperties.hasSubmission);
        ObjectProperties.createProperty(m, ObjectProperties.publishedIn, submission, venuePublication, ObjectProperties.hasPublication);

        ObjectProperties.createProperty(m, ObjectProperties.belongsTo, venuePublication, venue, ObjectProperties.publishes);

        ObjectProperties.createProperty(m, ObjectProperties.manages, handler, venue, ObjectProperties.managedBy);

        ObjectProperties.createProperty(m, ObjectProperties.assigns, handler, revision, ObjectProperties.assignedBy);
        ObjectProperties.createProperty(m, ObjectProperties.doneBy, revision, reviewer, ObjectProperties.takesPartIn);
        ObjectProperties.createProperty(m, ObjectProperties.reviews, revision, submission, ObjectProperties.reviewedBy);

        ObjectProperties.createProperty(m, ObjectProperties.paperRelatedTo, paper, field);
        ObjectProperties.createProperty(m, ObjectProperties.venueRelatedTo, venue, field);

        ObjectProperties.createProperty(m, ObjectProperties.takesPlaceIn, proceedings, location);

        // --------------------
        // -----Attributes-----
        // --------------------

        // People attributes
        DatatypeProperty name = DataProperties.createStringAttribute(m, DataProperties.name, academic);
        name.addSuperProperty(RDFS.label);

        // Paper attributes
        DataProperties.createStringAttribute(m, DataProperties.doi, paper);
        DatatypeProperty paperTitle = DataProperties.createStringAttribute(m, DataProperties.title, paper);
        paperTitle.addSuperProperty(RDFS.label);
        DatatypeProperty paperAbstract = DataProperties.createStringAttribute(m, DataProperties.paperAbstract, paper);
        paperAbstract.addSuperProperty(RDFS.comment);

        // Submission attributes
        DataProperties.createAttribute(m, DataProperties.submissionDate, submission, XSDDatatype.XSDdate);
        DataProperties.createAttribute(m, DataProperties.submissionAcceptedDate, submission, XSDDatatype.XSDdate);

        // Revision attributes
        DataProperties.createBooleanAttribute(m, DataProperties.accepted, revision);
        DatatypeProperty revisionText = DataProperties.createBooleanAttribute(m, DataProperties.reviewText, revision);
        revisionText.addSuperProperty(RDFS.comment);
        DataProperties.createAttribute(m, DataProperties.revisionDateStart, revision, XSDDatatype.XSDdate);
        DataProperties.createAttribute(m, DataProperties.revisionDateEnd, revision, XSDDatatype.XSDdate);

        // Field attributes
        DatatypeProperty keyword = DataProperties.createStringAttribute(m, DataProperties.keyword, field);
        keyword.addSuperProperty(RDFS.label);

        // Venue attributes
        DatatypeProperty venueName = DataProperties.createStringAttribute(m, DataProperties.venueName, venue);
        venueName.addSuperProperty(RDFS.label);

        // Venue publication attributes
        DataProperties.createAttribute(m, DataProperties.year, venuePublication, XSDDatatype.XSDgYear);


        return m;
    }

    private static void extendModel(OntModel m) {

        // -----------------------------
        // ------Class Hierarchies------
        // -----------------------------

        Classes.makeClassesDisjoint(Arrays.asList(
                m.getOntClass(Classes.academic),
                m.getOntClass(Classes.venue),
                m.getOntClass(Classes.venuePublication),
                m.getOntClass(Classes.paper),
                m.getOntClass(Classes.submission),
                m.getOntClass(Classes.revision),
                m.getOntClass(Classes.field)
        ));

        Classes.makeCompleteSubclasses(m,
                m.getOntClass(Classes.academic),
                Arrays.asList(
                        m.getOntClass(Classes.author),
                        m.getOntClass(Classes.reviewer),
                        m.getOntClass(Classes.handler)
                )
        );

        Classes.makeCompleteSubclasses(m,
                m.getOntClass(Classes.handler),
                Arrays.asList(
                        m.getOntClass(Classes.chair),
                        m.getOntClass(Classes.editor)
                )
        );

        Classes.makeDisjointCompleteSubclasses(m,
                m.getOntClass(Classes.paper),
                Arrays.asList(
                        m.getOntClass(Classes.fullPaper),
                        m.getOntClass(Classes.shortPaper),
                        m.getOntClass(Classes.demoPaper),
                        m.getOntClass(Classes.poster)
                )
        );

        Classes.makeDisjointCompleteSubclasses(m,
                m.getOntClass(Classes.venue),
                Arrays.asList(
                        m.getOntClass(Classes.conference),
                        m.getOntClass(Classes.journal)
                )
        );

        Classes.makeDisjointCompleteSubclasses(m,
                m.getOntClass(Classes.conference),
                Arrays.asList(
                        m.getOntClass(Classes.regularConference),
                        m.getOntClass(Classes.workshop),
                        m.getOntClass(Classes.symposium),
                        m.getOntClass(Classes.expertGroup)
                )
        );

        Classes.makeDisjointCompleteSubclasses(m,
                m.getOntClass(Classes.venuePublication),
                Arrays.asList(
                        m.getOntClass(Classes.proceedings),
                        m.getOntClass(Classes.volume)
                )
        );


        // ------------------
        // ----Properties----
        // ------------------

        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.author), m.getObjectProperty(ObjectProperties.authors), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.paper), m.getObjectProperty(ObjectProperties.authoredBy), 1);

        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getObjectProperty(ObjectProperties.submittedTo), 1);
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getObjectProperty(ObjectProperties.ofPaper), 1);
        Restrictions.createMaxCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getObjectProperty(ObjectProperties.publishedIn), 1);

        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.venuePublication), m.getObjectProperty(ObjectProperties.belongsTo), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.proceedings), m.getObjectProperty(ObjectProperties.takesPlaceIn), 1);
        Restrictions.createPropertyRestriction(m, m.getObjectProperty(ObjectProperties.belongsTo), m.getOntClass(Classes.proceedings), m.getOntClass(Classes.conference));
        Restrictions.createPropertyRestriction(m, m.getObjectProperty(ObjectProperties.belongsTo), m.getOntClass(Classes.volume), m.getOntClass(Classes.journal));

        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.handler), m.getObjectProperty(ObjectProperties.manages), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.venue), m.getObjectProperty(ObjectProperties.managedBy), 1);

        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getObjectProperty(ObjectProperties.assignedBy), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getObjectProperty(ObjectProperties.doneBy), 2);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.reviewer), m.getObjectProperty(ObjectProperties.takesPartIn), 1);
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getObjectProperty(ObjectProperties.reviews), 1);
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getObjectProperty(ObjectProperties.reviewedBy), 1);

        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.paper), m.getObjectProperty(ObjectProperties.paperRelatedTo), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.venue), m.getObjectProperty(ObjectProperties.venueRelatedTo), 1);


        // --------------------
        // -----Attributes-----
        // --------------------

        // People attributes
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.academic), m.getDatatypeProperty(DataProperties.name), 1);

        // Paper attributes
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.paper), m.getDatatypeProperty(DataProperties.title), 1);
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.paper), m.getDatatypeProperty(DataProperties.paperAbstract), 1);
        Restrictions.createMaxCardinalityRestriction(m, m.getOntClass(Classes.paper), m.getDatatypeProperty(DataProperties.doi), 1);

        // Submission attributes
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getDatatypeProperty(DataProperties.submissionDate), 1);
        Restrictions.createMaxCardinalityRestriction(m, m.getOntClass(Classes.submission), m.getDatatypeProperty(DataProperties.submissionAcceptedDate), 1);

        // Revision attributes
        Restrictions.createMaxCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getDatatypeProperty(DataProperties.accepted), 1);
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getDatatypeProperty(DataProperties.revisionDateStart), 1);
        Restrictions.createMaxCardinalityRestriction(m, m.getOntClass(Classes.revision), m.getDatatypeProperty(DataProperties.revisionDateEnd), 1);

        // Field attributes
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.field), m.getDatatypeProperty(DataProperties.keyword), 1);

        // Venue attributes
        Restrictions.createMinCardinalityRestriction(m, m.getOntClass(Classes.venue), m.getDatatypeProperty(DataProperties.venueName), 1);

        // Venue publication attributes
        Restrictions.createCardinalityRestriction(m, m.getOntClass(Classes.venuePublication), m.getDatatypeProperty(DataProperties.year), 1);
    }

}
