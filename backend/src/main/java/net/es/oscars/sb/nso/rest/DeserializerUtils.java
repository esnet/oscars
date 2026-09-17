package net.es.oscars.sb.nso.rest;

import lombok.Data;
import net.es.topo.common.dto.nso.IetfRestconfErrorResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.ObjectNode;


public class DeserializerUtils {

    @JsonDeserialize(using = LiveStatusErrorDeserializer.class)
    @Data
    public class EitherLiveStatusOrError {
        private LiveStatusOutput statusOutput;
        private IetfRestconfErrorResponse errorResponse;
    }

    public class LiveStatusErrorDeserializer extends ValueDeserializer<EitherLiveStatusOrError> {
        @Override
        public EitherLiveStatusOrError deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            EitherLiveStatusOrError either = new EitherLiveStatusOrError();
            ObjectMapper mapper = (ObjectMapper) jp.objectReadContext();
            ObjectNode root = (ObjectNode) mapper.readTree(jp);
            if (root.has("esnet-status:output")) {
                either.setStatusOutput(mapper.treeToValue(root, LiveStatusOutput.class));
            } else {
                either.setErrorResponse(mapper.treeToValue(root, IetfRestconfErrorResponse.class));
            }
            return either;
        }
    }
}
