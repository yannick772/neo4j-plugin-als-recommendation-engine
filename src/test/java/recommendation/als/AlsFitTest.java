package recommendation.als;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import recommendation.models.neo4j.CharacteristicVector;

import java.util.ArrayList;
import java.util.List;

public class AlsFitTest extends AlsBaseTest<AlsFit> {

    @Test
    public void testFit() {
//        List<CharacteristicVector> expected = new ArrayList<>();
//        expected.add(new CharacteristicVector(0.8062310925382973, 1.0391361380256263, 0.5293929485118679, 1.0929802822782206, 0.20962199234459788, 0.4135630561840833, 0.6089490998940263, 0.8315643131364294, 0.7598981532839105, 0.22175489860085196));
//        expected.add(new CharacteristicVector(0.6352570072800439, 0.9494910996343053, 0.5184529444161565, 0.8807249213730264, 0.5660802962376443, 0.5158780687866589, 0.4585663806856074, 0.8152063804696852, 0.5827792714097804, 0.34617778331697274));
//        expected.add(new CharacteristicVector(0.5613008775394527, 1.115590570882678, 0.41779853992626975, 0.4905878485269766, 0.4671846871348711, 0.06213774978539563, 0.15472533677113764, 0.7664792468473166, 0.6663954346558266, -0.10264688509599285));
//        expected.add(new CharacteristicVector(0.31989462765418136, 0.5324213417667245, 0.29922804305345596, 0.8633780052480188, 0.5079420362079357, 0.951280485354889, 0.6386545200477073, 0.5422736951889543, 0.254089277237987, 0.909066265796574));
//        expected.add(new CharacteristicVector(0.6861145893436777, 0.8885300786782289, 0.38665272182063165, 1.0358455646151132, -0.01622501080487604, 0.3877687355868135, 0.6747331383987916, 0.5930115743762292, 0.4155822797042974, 0.2631242747725605));
//        expected.add(new CharacteristicVector(0.8047267620470151, 0.17769885509989924, 0.6830941077076573, 1.304987173138036, 0.30001613458376086, 0.6956972946548532, 0.67069400236258, 0.6677607388411957, 0.5651455182757423, 0.4519068120797448));
//        expected.add(new CharacteristicVector(0.5678843305727804, 0.1946282572207798, 0.6458171818948835, 1.0071635910368637, 0.7915638034920152, 0.821074790601952, 0.48990098190577996, 0.6476325967126385, 0.23299443712973347, 0.6235785088071086));
//        expected.add(new CharacteristicVector(0.30347919320783906, -0.251236802901893, 0.2578029430146398, 0.9031913459656385, 0.0381220445847511, 0.9312398256854989, 0.5873301734023384, 0.4039654638076152, 0.885437083125745, 0.8480568832744827));
//        expected.add(new CharacteristicVector(0.41594481432410224, 0.420068534473822, 0.33172257002404604, 0.9058577963091567, 0.29559928538354574, 0.7676309831040323, 0.6185551922805432, 0.4977010623079138, 0.3264889152013466, 0.6896839524038414));
//        expected.add(new CharacteristicVector(0.5318515506366492, 1.0812112581247793, 0.3147286858839805, 0.9536607319284429, 0.26269380976486, 0.6277662309557617, 0.7090045016423876, 0.6273364127133744, 0.23859853695891886, 0.567071191703374));

        ObjectMapper objectMapper = new ObjectMapper();
        try (Session session = driver.session()) {
            // arrange
            loadFauxDataset("testDatabase");

            // act
            Record record = session.run("CALL recommendation.als.fit(\"User\", \"Movie\", \"RATING\", \"rating\", 100, 1, 10, 12345678) YIELD value RETURN value").single();

            // assert
            List<CharacteristicVector> factors = objectMapper.readValue(objectMapper.writeValueAsString(
                    session.run("MATCH (u:User) RETURN u.als AS factors ORDER BY id(u) ASC")
                            .stream()
                            .map(Record::asMap)
                            .toList()
            ), objectMapper.getTypeFactory().constructCollectionType(List.class, CharacteristicVector.class));
            Assertions.assertThat(record.get("value").asBoolean()).isTrue();
            Assertions.assertThat(factors).isNotEmpty();
            Assertions.assertThat(factors).allMatch(x -> x.getLength() == 10);
//            Assertions.assertThat(factors)
//                    .usingRecursiveFieldByFieldElementComparator()
//                    .isEqualTo(expected);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}