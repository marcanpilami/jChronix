import { default as React } from "react";
import { default as Navbar } from "react-bootstrap/Navbar";
import { default as Nav } from "react-bootstrap/Nav";
import { LinkContainer } from "react-router-bootstrap";

export function NavMenu() {
  return (
    <Navbar bg="light" expand="lg">
      <Navbar.Brand>Chronix</Navbar.Brand>
      <Navbar.Toggle aria-controls="basic-navbar-nav" />
      <Navbar.Collapse id="basic-navbar-nav">
        <Nav className="mr-auto">
          <LinkContainer to="./network">
            <Nav.Link>Network</Nav.Link>
          </LinkContainer>
          <LinkContainer to="./application">
            <Nav.Link>Applications</Nav.Link>
          </LinkContainer>
          <LinkContainer to="./run">
            <Nav.Link>Run</Nav.Link>
          </LinkContainer>
        </Nav>
      </Navbar.Collapse>
    </Navbar>
  );
}
