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
        List<CharacteristicVector> expected = new ArrayList<>();
        expected.add(new CharacteristicVector(0.7223873734474182, 0.1819041520357132, -0.28258609771728516, -0.5899862051010132, 0.0613073892891407, 0.5782291293144226, -1.2125462293624878, -4.675524542108178E-4, 0.7874037623405457, 0.9116666913032532));
        expected.add(new CharacteristicVector(0.837136447429657, 0.28485432267189026, -0.31790363788604736, -0.31679436564445496, 0.28111109137535095, 0.8247120976448059, -1.447192668914795, -0.14091208577156067, -0.16356049478054047, 0.7342073321342468));
        expected.add(new CharacteristicVector(0.7432504892349243, 0.29948243498802185, -0.23423539102077484, -0.6788914203643799, 0.1553056538105011, 0.5131173729896545, -0.6582404971122742, 0.06204855069518089, 0.6460397243499756, 0.7825377583503723));
        expected.add(new CharacteristicVector(0.9101999402046204, 0.4985541105270386, -0.2439170926809311, -0.4748895764350891, 0.49374204874038696, 0.602176308631897, -1.169019103050232, -0.3910790979862213, -0.18863968551158905, 0.5524665713310242));
        expected.add(new CharacteristicVector(0.8779259324073792, 0.3703073561191559, -0.19207704067230225, -0.1126464307308197, 0.3879876434803009, 0.7237114310264587, -1.3563096523284912, -0.2089112102985382, 0.17780150473117828, 0.7473862767219543));
        expected.add(new CharacteristicVector(1.071677803993225, 0.658233106136322, -0.22882682085037231, -0.4113006293773651, 0.6385825872421265, 0.7610313296318054, -0.9924386143684387, -0.36910656094551086, -0.4024682939052582, 0.5480273962020874));
        expected.add(new CharacteristicVector(0.9014312028884888, 0.5400746464729309, -0.3763425052165985, -0.8059692978858948, 0.21409866213798523, 0.8577635288238525, -0.9668055176734924, -0.2617534101009369, -0.0351727157831192, 0.6057169437408447));
        expected.add(new CharacteristicVector(0.9861198663711548, 0.6609185338020325, -0.12156841903924942, -0.9275709390640259, 0.7224038243293762, 0.08674567937850952, -0.5258398652076721, -0.46717724204063416, 0.3146415054798126, 0.5178998112678528));
        expected.add(new CharacteristicVector(0.8582730889320374, 0.5476943850517273, -0.2059149593114853, -0.45769476890563965, 0.5113548636436462, 0.5215920209884644, -1.0292876958847046, -0.4830370247364044, -0.220846489071846, 0.4215657114982605));
        expected.add(new CharacteristicVector(0.8148996829986572, 0.41737881302833557, -0.13209286332130432, 0.15596824884414673, 0.39017847180366516, 0.7570310235023499, -1.3862872123718262, -0.33711057901382446, 0.10467924177646637, 0.6056724786758423));

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
            Assertions.assertThat(factors)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(expected);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}