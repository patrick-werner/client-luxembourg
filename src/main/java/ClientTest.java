

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;

public class ClientTest {

  /**
   * This is the Java main method, which gets executed
   */
  public static void main(String[] args) {

    // Create a context
    FhirContext ctx = FhirContext.forR4();

    // Create a client
    IGenericClient client = ctx.newRestfulGenericClient("https://hapi.fhir.org/baseR4");
    client.registerInterceptor(new LoggingInterceptor());

    // Read a patient with the given ID
    Patient patient = client.read().resource(Patient.class).withId("example").execute();

    // Print the output
    IParser jsonParser = ctx.newJsonParser().setPrettyPrint(true);
    String string = jsonParser.encodeResourceToString(patient);
    System.out.println(string);

    // search for Patient smith

    Bundle smith = client.search()
        .forResource(Patient.class)
        .where(Patient.NAME.matches().value("Smith"))
        .returnBundle(Bundle.class)
        .execute();

    smith.getEntry().stream().map(e -> e.getResource())
        .forEach(r -> System.out.println("ResourceId: " + r.getId()));
    System.out.println("Total: " + smith.getTotal());

  // create a patient
    Patient patient2 = new Patient();
    patient2.addName().addGiven("Patrick").setFamily("Nachname");
    patient2.setActive(true);
    patient2.addIdentifier().setSystem("http://example.org/sid/test").setValue("123");

    MethodOutcome methodOutcome = client.create().resource(patient2).execute();
    IIdType id = methodOutcome.getId();
    System.out.println("Assigned ID to Patient: " + id);

    // read posted resource
    Patient readPatient = client.read().resource(Patient.class).withId(id).execute();
//    System.out.println(jsonParser.encodeResourceToString(readPatient));

    // update the Patient
    readPatient.addName().addGiven("newName").setFamily("newFamily");
    MethodOutcome execute = client.update().resource(readPatient).execute();
    System.out.println(execute.getId());

    MethodOutcome execute1 = client.delete().resourceById(id).execute();
    OperationOutcome operationOutcome = (OperationOutcome) execute1.getOperationOutcome();
    System.out.println(jsonParser.encodeResourceToString(operationOutcome));

    try {
      Patient patientAfterDeletion = client.read().resource(Patient.class).withId(id.getIdPart())
          .execute();
    }
    catch (ResourceGoneException e) {
      System.err.println("Resource: Patient/" + id.getIdPart() + " was deleted");
    }

    // search by url
    client.search().byUrl("Patient?name=Patrick&name=Fritz").execute();

  }
}