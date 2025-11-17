import './App.css'
import "primereact/resources/themes/vela-blue/theme.css";
import 'primeicons/primeicons.css';
import Search from './components/Search';
import { useState } from 'react';
import { Button } from 'primereact/button';
import Add from './components/Add';
import { PrimeReactProvider } from 'primereact/api';

function App() {
  const [view, setView] = useState<"Search" | "Add">("Search");
  return (
    <PrimeReactProvider>
      <div className="menu">
        {view !== "Search" && <Button onClick={() => setView("Search")} title="Show search interface"><i className="pi pi-search"></i></Button>}
        {view !== "Add" && <Button onClick={() => setView("Add")} title="Import new album"><i className="pi pi-plus"></i></Button>}
      </div>
      {view === "Search" && <Search/>}
      {view === "Add" && <Add/>}
    </PrimeReactProvider>
  )
}

export default App
