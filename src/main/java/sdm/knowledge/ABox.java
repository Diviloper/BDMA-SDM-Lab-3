package sdm.knowledge;


import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class ABox {
    private static final Random random = new Random();

    private final OntModel model;
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Set<String>> authorNames = new HashMap<>();
    private final Map<String, String> venueNames = new HashMap<>();
    private final Map<String, Individual> papers = new HashMap<>();
    private final Map<String, Individual> paperSubmissions = new HashMap<>();
    private final Map<String, Individual> paperRevisions = new HashMap<>();
    private final Map<String, Individual> fields = new HashMap<>();

    public ABox(OntModel model) {
        this.model = model;
    }

    public static void main(String[] args) throws IOException, CsvValidationException {
        if (args.length < 3) {
            System.out.println("Invalid arguments");
            System.out.println("Usage: java ABox <TBOX path> <resources path> <output path>");
            System.out.println("\t <TBOX path>:        path to ttl file containing the TBOX generated with the TBOX class.");
            System.out.println("\t                     ./src/main/resources/tbox.ttl");
            System.out.println("\t <resources path>:   path to resources folder containing required csvs (data, citations reviews and reviewers)");
            System.out.println("\t                     ./src/main/resources/");
            System.out.println("\t <output path>:      path to file where the ABOX triples will be saved to.");
            System.out.println("\t                     ./src/main/resources/abox.ttl");
        }
        String modelFilePath = args[0];
        String dataFolder = args[1];
        String outputPath = args[2];

        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFDataMgr.read(model, modelFilePath);


        ABox abox = new ABox(model);
        abox.populateModel(dataFolder);

        FileOutputStream output = new FileOutputStream(outputPath);
        RDFDataMgr.write(output, model, RDFFormat.TURTLE);
    }

    public void populateModel(String dataPath) throws IOException, CsvValidationException {

        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(dataPath + "data.csv"));
        Map<String, String> values;
        while ((values = reader.readMap()) != null) {
            boolean conference = values.get("Document Type").equals("Conference Paper");

            // Paper
            Individual paper = createPaper(model, values, conference);

            // Authors
            addPaperAuthors(model, values, paper);

            // Venue
            Individual venue = createVenue(model, values, conference);

            // Venue Publication
            Individual venuePublication = createVenuePublication(model, venue, values, conference);

            // Submission
            Individual submission = createSubmission(model, paper, venue, venuePublication, values.get("Year"));

            // Fields
            createFields(model, values, paper, venue);

            paperSubmissions.put(values.get("DOI"), submission);
        }

        reader = new CSVReaderHeaderAware(new FileReader(dataPath + "citations.csv"));
        while ((values = reader.readMap()) != null) {
            createCitation(values.get("Paper"), values.get("Citation"));
        }

        reader = new CSVReaderHeaderAware(new FileReader(dataPath + "reviews.csv"));
        while ((values = reader.readMap()) != null) {
            if (values.get("Decision").equals("False") || paperRevisions.containsKey(values.get("Paper"))) continue;
            createRevision(values);
        }

        reader = new CSVReaderHeaderAware(new FileReader(dataPath + "reviewers.csv"));
        while ((values = reader.readMap()) != null) {
            addReviewers(values);
        }

    }

    private void addReviewers(Map<String, String> values) {
        ObjectProperty doneBy = model.getObjectProperty(TBox.ObjectProperties.doneBy);
        Individual revision = paperRevisions.get(values.get("Paper"));
        for (String reviewer : values.get("Reviewers").split(";")) {
            revision.addProperty(doneBy, model.getIndividual(name("A", reviewer)));
        }
    }

    private void createRevision(Map<String, String> values) {
        OntClass revisionClass = model.getOntClass(TBox.Classes.revision);
        ObjectProperty reviews = model.getObjectProperty(TBox.ObjectProperties.reviews);
        DatatypeProperty accepted = model.getDatatypeProperty(TBox.DataProperties.accepted);
        DatatypeProperty reviewText = model.getDatatypeProperty(TBox.DataProperties.reviewText);
        OntProperty managedBy = model.getObjectProperty(TBox.ObjectProperties.managedBy);
        OntProperty assigns = model.getObjectProperty(TBox.ObjectProperties.assigns);
        OntProperty submittedTo = model.getObjectProperty(TBox.ObjectProperties.submittedTo);
        OntProperty startDate = model.getDatatypeProperty(TBox.DataProperties.revisionDateStart);
        OntProperty endDate = model.getDatatypeProperty(TBox.DataProperties.revisionDateEnd);

        Individual revision = revisionClass.createIndividual(autoName("R"));
        revision.addLiteral(accepted, model.createTypedLiteral(true));
        revision.addLiteral(reviewText, model.createTypedLiteral(values.get("Review")));

        Individual submission = paperSubmissions.get(values.get("Paper"));
        revision.addProperty(reviews, submission);

        String year = submission.getPropertyValue(model.getDatatypeProperty(TBox.DataProperties.submissionDate)).toString().split("-")[0];
        revision.addProperty(startDate, model.createTypedLiteral(year + "-01-10", XSDDatatype.XSDdate));
        revision.addProperty(endDate, model.createTypedLiteral(year + "-04-01", XSDDatatype.XSDdate));

        Individual venue = submission.getPropertyValue(submittedTo).as(Individual.class);
        Individual handler = venue.listPropertyValues(managedBy).toList().get(random.nextInt(3)).as(Individual.class);
        handler.addProperty(assigns, revision);


        paperRevisions.put(values.get("Paper"), revision);
    }

    private void createCitation(String paper, String citation) {
        ObjectProperty cites = model.getObjectProperty(TBox.ObjectProperties.cites);
        Individual citer = papers.get(paper);
        Individual cited = papers.get(citation);
        citer.addProperty(cites, cited);
    }

    private void createFields(OntModel model, Map<String, String> values, Individual paper, Individual venue) {
        OntClass fieldClass = model.getOntClass(TBox.Classes.field);
        OntProperty keywordProp = model.getDatatypeProperty(TBox.DataProperties.keyword);
        OntProperty paperRelatedTo = model.getObjectProperty(TBox.ObjectProperties.paperRelatedTo);
        OntProperty venueRelatedTo = model.getObjectProperty(TBox.ObjectProperties.venueRelatedTo);

        for (String keyword : values.get("Index Keywords").split("; ")) {
            Individual field;
            if (!fields.containsKey(keyword)) {
                field = fieldClass.createIndividual(autoName("F"));
                field.addLiteral(keywordProp, model.createTypedLiteral(keyword));
                fields.put(keyword, field);
            }
            field = fields.get(keyword);
            venue.addProperty(venueRelatedTo, field);
            paper.addProperty(paperRelatedTo, field);
        }
    }

    @NotNull
    private Individual createSubmission(OntModel model, Individual paper, Individual venue, Individual venuePublication, String year) {
        OntClass submissionClass = model.getOntClass(TBox.Classes.submission);
        OntProperty submittedAs = model.getObjectProperty(TBox.ObjectProperties.submittedAs);
        OntProperty submittedTo = model.getObjectProperty(TBox.ObjectProperties.submittedTo);
        OntProperty publishedIn = model.getObjectProperty(TBox.ObjectProperties.publishedIn);
        OntProperty submissionDate = model.getDatatypeProperty(TBox.DataProperties.submissionDate);
        OntProperty acceptedDate = model.getDatatypeProperty(TBox.DataProperties.submissionAcceptedDate);

        Individual submission = submissionClass.createIndividual(autoName("Sub"));
        paper.addProperty(submittedAs, submission);
        submission.addProperty(submittedTo, venue);
        submission.addProperty(publishedIn, venuePublication);
        submission.addLiteral(submissionDate, model.createTypedLiteral(year + "-01-01", XSDDatatype.XSDdate));
        submission.addLiteral(acceptedDate, model.createTypedLiteral(year + "-04-02", XSDDatatype.XSDdate));

        return submission;
    }

    @NotNull
    private Individual createVenuePublication(OntModel model, Individual venue, Map<String, String> values, boolean conference) {
        OntClass volumeClass = model.getOntClass(TBox.Classes.volume);
        OntClass proceedingsClass = model.getOntClass(TBox.Classes.proceedings);
        OntClass locationClass = model.getOntClass(TBox.Classes.location);
        OntProperty venuePublicationYear = model.getDatatypeProperty(TBox.DataProperties.year);
        OntProperty volumeNumber = model.getDatatypeProperty(TBox.DataProperties.volumeNumber);
        OntProperty belongsTo = model.getObjectProperty(TBox.ObjectProperties.belongsTo);
        OntProperty takesPlaceIn = model.getObjectProperty(TBox.ObjectProperties.takesPlaceIn);

        OntClass venuePublicationClass = conference ? proceedingsClass : volumeClass;
        String vName = venueNames.get(values.get("Source title"));
        Individual venuePublication = venuePublicationClass.createIndividual(vName + "-" + values.get(conference ? "Year" : "Volume"));
        if (!venuePublication.hasProperty(venuePublicationYear)) {
            venuePublication.addLiteral(venuePublicationYear, model.createTypedLiteral(values.get("Year"), XSDDatatype.XSDgYear));
        }

        venuePublication.addProperty(belongsTo, venue);
        if (conference) {
            venuePublication.addProperty(takesPlaceIn, locationClass.createIndividual(TBox.DBPO + "Barcelona"));
        } else {
            venuePublication.addProperty(volumeNumber, model.createTypedLiteral(values.get("Volume"), XSDDatatype.XSDunsignedInt));
        }

        return venuePublication;
    }

    private Individual createVenue(OntModel model, Map<String, String> values, boolean conference) {
        List<OntClass> conferenceSubclasses = Arrays.asList(
                model.getOntClass(TBox.Classes.regularConference),
                model.getOntClass(TBox.Classes.workshop),
                model.getOntClass(TBox.Classes.symposium),
                model.getOntClass(TBox.Classes.expertGroup)
        );
        OntClass journalClass = model.getOntClass(TBox.Classes.journal);
        OntProperty venueName = model.getDatatypeProperty(TBox.DataProperties.venueName);

        OntClass venueClass = conference ? conferenceSubclasses.get(random.nextInt(4)) : journalClass;
        OntClass handlerClass = model.getOntClass(conference ? TBox.Classes.chair : TBox.Classes.editor);
        OntProperty managedBy = model.getObjectProperty(TBox.ObjectProperties.managedBy);

        String vName = values.get("Source title");
        Individual venue;
        if (!venueNames.containsKey(vName)) {
            venueNames.put(vName, autoName("V"));
            venue = venueClass.createIndividual(venueNames.get(vName));
            venue.addLiteral(venueName, model.createTypedLiteral(vName));

            for (int i = 0; i < 3; i++) {
                Individual handler = handlerClass.createIndividual(autoName("H"));
                venue.addProperty(managedBy, handler);
            }
        } else {
            venue = model.getIndividual(venueNames.get(vName));
        }
        return venue;
    }

    private void addPaperAuthors(OntModel model, Map<String, String> values, Individual paper) {
        List<String> authors = Arrays.stream(values.get("Authors").split(", ")).toList();
        List<String> authorsId = Arrays.stream(values.get("Author(s) ID").split(";")).toList();
        for (int i = 0; i < authors.size(); i++) {
            createAuthorPaper(model, authorsId.get(i), authors.get(i), paper);
        }
    }

    @NotNull
    private Individual createPaper(OntModel model, Map<String, String> values, boolean conference) {
        List<OntClass> paperSubclasses = Arrays.asList(
                model.getOntClass(TBox.Classes.fullPaper),
                model.getOntClass(TBox.Classes.shortPaper),
                model.getOntClass(TBox.Classes.demoPaper),
                model.getOntClass(TBox.Classes.poster)
        );

        OntProperty paperDOI = model.getDatatypeProperty(TBox.DataProperties.doi);
        OntProperty paperTitle = model.getDatatypeProperty(TBox.DataProperties.title);
        OntProperty paperAbstract = model.getDatatypeProperty(TBox.DataProperties.paperAbstract);

        OntClass paperClass = paperSubclasses.get(random.nextInt(conference ? 4 : 3));
        Individual paper = paperClass.createIndividual(autoName("P"));
        paper.addLiteral(paperDOI, model.createTypedLiteral(values.get("DOI")));
        paper.addLiteral(paperTitle, model.createTypedLiteral(values.get("Title")));
        paper.addLiteral(paperAbstract, model.createTypedLiteral(values.get("Abstract")));

        papers.put(values.get("DOI"), paper);
        return paper;
    }

    private void createAuthorPaper(OntModel model, String id, String name, Individual paper) {
        OntClass authorClass = model.getOntClass(TBox.Classes.author);
        OntProperty personName = model.getDatatypeProperty(TBox.DataProperties.name);
        OntProperty authorsPaper = model.getObjectProperty(TBox.ObjectProperties.authors);

        String aid = name("A", id);

        Individual author;
        if (!authorNames.containsKey(aid)) {
            authorNames.put(aid, new HashSet<>());
        }
        author = authorClass.createIndividual(aid);

        if (!authorNames.get(aid).contains(name)) {
            author.addLiteral(personName, model.createTypedLiteral(name));
            authorNames.get(aid).add(name);
        }

        author.addProperty(authorsPaper, paper);
    }

    private String name(String prefix, String id) {
        return TBox.NS + prefix + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    private String name(String id) {
        return TBox.NS + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    private String autoName(String prefix) {
        counters.putIfAbsent(prefix, 0);
        String n = name(prefix + counters.get(prefix));
        counters.put(prefix, counters.get(prefix) + 1);
        return n;
    }
}