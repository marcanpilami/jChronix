import React, { useEffect, useState } from "react";

const chronixData = {
  applicationList: [],
  currentApplication: null,
  network: null,
  refreshData: () => {},
};

export const ChronixDataContext = React.createContext(chronixData);

export function ChronixDataContextProvider({ children }) {
  const [network, setNetwork] = useState({});

  // Load data on startup
  useEffect(() => {
    fetch("/ws/meta/environment", {
      method: "GET",
      cache: "no-cache",
    })
      .then((response) => response.json())
      .then((data) => setNetwork(data));
  }, []);

  return (
    <ChronixDataContext.Provider value={{ network: network }}>
      {children}
    </ChronixDataContext.Provider>
  );
}
