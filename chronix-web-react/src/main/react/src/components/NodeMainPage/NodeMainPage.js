import { default as Row } from "react-bootstrap/Row";
import { default as Col } from "react-bootstrap/Col";
import { useContext } from "react";
import { ChronixDataContext } from "../../contexts/ChronixDataContext";
import { Loading } from "../Loading/Loading";

export function NodeMainPage() {
  const chronixData = useContext(ChronixDataContext);

  return (
    <Row>
      <Col>
        <h2>Places</h2>
        {chronixData.network.places ? (
          <>
            {chronixData.network.places.map((p, i) => (
              <PlaceData place={p} key={i} />
            ))}
          </>
        ) : (
          <Loading />
        )}

        <h2>Nodes</h2>
        {chronixData.network.nodes ? (
          <>
            {chronixData.network.nodes.map((p, i) => (
              <NodeData node={p} key={i} />
            ))}
          </>
        ) : (
          <Loading />
        )}
      </Col>
    </Row>
  );
}

function PlaceData({ place }) {
  return (
    <div>
      ID: {place.id} - Name: {place.name}
    </div>
  );
}

function NodeData({ node }) {
  return (
    <div>
      ID: {node.id} - DNS: {node.dns}
    </div>
  );
}
