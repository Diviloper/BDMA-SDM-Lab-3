package sdm.knowledge;


import com.opencsv.CSVReaderHeaderAware;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.URIref;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class ABox {
    private static final Random random = new Random();
    private static final Map<String, Integer> counters = new HashMap<>();
    private static final Map<String, Set<String>> authorNames = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Arguments: <Input model TBOX> <Data folder> <Output file>");
        }
        String modelFilePath = args[0];
        String dataFolder = args[1];
        String outputPath = args[2];

        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
        RDFDataMgr.read(model, modelFilePath);

        populateModel(model, dataFolder);
    }

    public static void populateModel(OntModel model, String dataPath) throws IOException {
        // Papers, authors and venues

        Map<String, OntClass> conferenceTypes = new HashMap<>();
        List<OntClass> conferenceSubclasses = Arrays.asList(
                model.getOntClass(TBox.Classes.regularConference),
                model.getOntClass(TBox.Classes.workshop),
                model.getOntClass(TBox.Classes.symposium),
                model.getOntClass(TBox.Classes.expertGroup)
        );
        OntClass journalClass = model.getOntClass(TBox.Classes.journal);
        OntProperty venueName = model.getDatatypeProperty(TBox.DataProperties.venueName);

        CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(dataPath + "data.csv"));
        Map<String, String> values;
        while ((values = reader.readMap()) != null) {
            boolean conference = values.get("Document Type").equals("Conference Paper");

            // Paper
            Individual paper = createPaper(model, values, conference);

            // Authors
            List<String> authors = Arrays.stream(values.get("Authors").split(", ")).toList();
            List<String> authorsId = Arrays.stream(values.get("Author(s) ID").split(";")).toList();
            for (int i = 0; i < authors.size(); i++) {
                createAuthorPaper(model, authorsId.get(i), authors.get(i), paper);
            }

            // Venue
            OntClass venueClass = conference ? conferenceSubclasses.get(random.nextInt(4)) : journalClass;
            Individual venue = venueClass.createIndividual(autoName("V"));
            venue.addLiteral(venueName, model.createTypedLiteral(values.get("Source title")));

        }
    }

    @NotNull
    private static Individual createPaper(OntModel model, Map<String, String> values, boolean conference) {
        List<OntClass> paperSubclasses = Arrays.asList(
                model.getOntClass(TBox.Classes.fullPaper),
                model.getOntClass(TBox.Classes.shortPaper),
                model.getOntClass(TBox.Classes.demoPaper),
                model.getOntClass(TBox.Classes.poster)
        );

        OntProperty submittedAs = model.getObjectProperty(TBox.ObjectProperties.submittedAs);
        OntProperty paperTitle = model.getDatatypeProperty(TBox.DataProperties.title);
        OntProperty paperAbstract = model.getDatatypeProperty(TBox.DataProperties.paperAbstract);
        OntClass submissionClass = model.getOntClass(TBox.Classes.submission);

        OntClass paperClass = paperSubclasses.get(random.nextInt(conference ? 4 : 3));
        Individual paper = paperClass.createIndividual(name(values.get("DOI")));
        Individual submission = submissionClass.createIndividual(autoName("Sub"));
        paper.addProperty(submittedAs, submission);
        paper.addLiteral(paperTitle, model.createTypedLiteral(values.get("Title")));
        paper.addLiteral(paperAbstract, model.createTypedLiteral(values.get("Abstract")));

        return paper;
    }

    private static void createAuthorPaper(OntModel model, String id, String name, Individual paper) {
        OntClass authorClass = model.getOntClass(TBox.Classes.author);
        OntProperty personName = model.getDatatypeProperty(TBox.DataProperties.name);
        OntProperty authorsPaper = model.getObjectProperty(TBox.ObjectProperties.authors);

        String aid = name("A", id);

        Individual author;
        if (authorNames.containsKey(aid)) {
            author = model.getIndividual(aid);
        } else {
            authorNames.put(aid, new HashSet<>());
            author = authorClass.createIndividual(aid);
        }

        if (!authorNames.get(aid).contains(name)) {
            author.addLiteral(personName, model.createTypedLiteral(name));
            authorNames.get(aid).add(name);
        }

        author.addProperty(authorsPaper, paper);
    }

    private static String name(String prefix, String id) {
        return TBox.NS + prefix + URIref.encode(id);
    }

    private static String name(String id) {
        return TBox.NS + URIref.encode(id);
    }

    private static String autoName(String prefix) {
        counters.putIfAbsent(prefix, 0);
        String n = name(prefix + counters.get(prefix));
        counters.put(prefix, counters.get(prefix) + 1);
        return n;
    }
}
