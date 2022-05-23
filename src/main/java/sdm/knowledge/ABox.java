package sdm.knowledge;


import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.URIref;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class ABox {
    private static final Random random = new Random();
    
    private final OntModel model;
    private final Map<String, OntClass> classCache = new HashMap<>();
    private final Map<String, DatatypeProperty> dataPropCache = new HashMap<>();
    private final Map<String, ObjectProperty> objPropCache = new HashMap<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Set<String>> authorNames = new HashMap<>();
    private final Map<String, String> venueNames = new HashMap<>();
    private final Map<String, Set<Individual>> venueHandlers = new HashMap<>();
    private final Map<String, Individual> paperSubmissions = new HashMap<>();
    private final Map<String, Individual> fields = new HashMap<>();

    public ABox(OntModel model) {
        this.model = model;
    }

    public static void main(String[] args) throws IOException, CsvValidationException {
        if (args.length < 3) {
            System.out.println("Arguments: <Input model TBOX> <Data folder> <Output file>");
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
            Individual submission = createSubmission(model, paper, venue, venuePublication);

            // Fields
            createFields(model, values, paper, venue);

            paperSubmissions.put(values.get("DOI"), submission);
        }


    }

    private void createFields(OntModel model, Map<String, String> values, Individual paper, Individual venue) {
        OntClass fieldClass = getClass(TBox.Classes.field);
        OntProperty keywordProp = getDataProp(TBox.DataProperties.keyword);
        OntProperty paperRelatedTo = getObjProp(TBox.ObjectProperties.paperRelatedTo);
        OntProperty venueRelatedTo = getObjProp(TBox.ObjectProperties.venueRelatedTo);

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
    private Individual createSubmission(OntModel model, Individual paper, Individual venue, Individual venuePublication) {
        OntClass submissionClass = getClass(TBox.Classes.submission);
        OntProperty submittedAs = getObjProp(TBox.ObjectProperties.submittedAs);
        OntProperty submittedTo = getObjProp(TBox.ObjectProperties.submittedTo);
        OntProperty publishedIn = getObjProp(TBox.ObjectProperties.publishedIn);

        Individual submission = submissionClass.createIndividual(autoName("Sub"));
        paper.addProperty(submittedAs, submission);
        submission.addProperty(submittedTo, venue);
        submission.addProperty(publishedIn, venuePublication);
        return submission;
    }

    @NotNull
    private Individual createVenuePublication(OntModel model, Individual venue, Map<String, String> values, boolean conference) {
        OntClass volumeClass = getClass(TBox.Classes.volume);
        OntClass proceedingsClass = getClass(TBox.Classes.proceedings);
        OntProperty venuePublicationYear = getDataProp(TBox.DataProperties.year);
        OntProperty belongsTo = getObjProp(TBox.ObjectProperties.belongsTo);

        OntClass venuePublicationClass = conference ? proceedingsClass : volumeClass;
        String vName = venueNames.get(values.get("Source title"));
        Individual venuePublication = venuePublicationClass.createIndividual(name(vName, values.get(conference ? "Year" : "Volume")));
        venuePublication.addLiteral(venuePublicationYear, model.createTypedLiteral(values.get("Year"), XSDDatatype.XSDgYear));

        venuePublication.addProperty(belongsTo, venue);

        return venuePublication;
    }

    private Individual createVenue(OntModel model, Map<String, String> values, boolean conference) {
        List<OntClass> conferenceSubclasses = Arrays.asList(
                getClass(TBox.Classes.regularConference),
                getClass(TBox.Classes.workshop),
                getClass(TBox.Classes.symposium),
                getClass(TBox.Classes.expertGroup)
        );
        OntClass journalClass = getClass(TBox.Classes.journal);
        OntProperty venueName = getDataProp(TBox.DataProperties.venueName);

        OntClass venueClass = conference ? conferenceSubclasses.get(random.nextInt(4)) : journalClass;
        OntClass handlerClass = getClass(conference ? TBox.Classes.chair : TBox.Classes.editor);

        String vName = values.get("Source title");
        Individual venue;
        if (!venueNames.containsKey(vName)) {
            venueNames.put(vName, autoName("V"));
            venue = venueClass.createIndividual(venueNames.get(vName));
            venue.addLiteral(venueName, model.createTypedLiteral(vName));

            venueHandlers.put(vName, Set.of(
                    handlerClass.createIndividual(autoName("H")),
                    handlerClass.createIndividual(autoName("H")),
                    handlerClass.createIndividual(autoName("H"))));
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
                getClass(TBox.Classes.fullPaper),
                getClass(TBox.Classes.shortPaper),
                getClass(TBox.Classes.demoPaper),
                getClass(TBox.Classes.poster)
        );

        OntProperty paperTitle = getDataProp(TBox.DataProperties.title);
        OntProperty paperAbstract = getDataProp(TBox.DataProperties.paperAbstract);

        OntClass paperClass = paperSubclasses.get(random.nextInt(conference ? 4 : 3));
        Individual paper = paperClass.createIndividual(name(values.get("DOI")));
        paper.addLiteral(paperTitle, model.createTypedLiteral(values.get("Title")));
        paper.addLiteral(paperAbstract, model.createTypedLiteral(values.get("Abstract")));

        return paper;
    }

    private void createAuthorPaper(OntModel model, String id, String name, Individual paper) {
        OntClass authorClass = getClass(TBox.Classes.author);
        OntProperty personName = getDataProp(TBox.DataProperties.name);
        OntProperty authorsPaper = getObjProp(TBox.ObjectProperties.authors);

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
        return TBox.NS + prefix + URIref.encode(id);
    }

    private String name(String id) {
        return TBox.NS + URIref.encode(id);
    }

    private String autoName(String prefix) {
        counters.putIfAbsent(prefix, 0);
        String n = name(prefix + counters.get(prefix));
        counters.put(prefix, counters.get(prefix) + 1);
        return n;
    }
    
    private OntClass getClass(String name) {
        if (!classCache.containsKey(name)) {
            classCache.put(name, model.getOntClass(name));
        }
        return classCache.get(name);
    }
    private DatatypeProperty getDataProp(String name) {
        if (!dataPropCache.containsKey(name)) {
            dataPropCache.put(name, model.getDatatypeProperty(name));
        }
        return dataPropCache.get(name);
    }
    private ObjectProperty getObjProp(String name) {
        if (!objPropCache.containsKey(name)) {
            objPropCache.put(name, model.getObjectProperty(name));
        }
        return objPropCache.get(name);
    }
}