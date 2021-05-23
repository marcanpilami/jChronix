import "bootstrap/dist/css/bootstrap.min.css";
import { default as Container } from "react-bootstrap/Container";
import { BrowserRouter as Router } from "react-router-dom";
import "./App.css";
import { NavMenu } from "./components/NavMenu/NavMenu";
import { ChronixDataContextProvider } from "./contexts/ChronixDataContext";
import { Routes } from "./routes";

function App() {
  return (
    <div className="App">
      <Router basename="/static">
        <NavMenu />

        <ChronixDataContextProvider>
          <Container fluid>
            <Routes />
          </Container>
        </ChronixDataContextProvider>
      </Router>
    </div>
  );
}

export default App;
