import { Switch, Route } from "react-router-dom";
import { NodeMainPage } from "./components/NodeMainPage/NodeMainPage";

export function Routes() {
  return (
    <Switch>
      <Route path="/network">
        <NodeMainPage />
      </Route>
      <Route path="/application">
        <div>TODO</div>
      </Route>
      <Route path="/run">
        <div>TODO</div>
      </Route>

      <Route path="/" exact>
        <NodeMainPage />
      </Route>
    </Switch>
  );
}
