import './App.css'
import "primereact/resources/themes/vela-blue/theme.css";
import 'primeicons/primeicons.css';
import Search from './components/Search';
import { useState } from 'react';
import { Button } from 'primereact/button';
import Add from './components/Add';
import { PrimeReactProvider } from 'primereact/api';
import TagsAlt from './components/TagsAlt';
import { useTranslation } from 'react-i18next';

type VIEWS = "VIEW_SEARCH" | "VIEW_ADD" | "VIEW_TAGS";

function App() {
  const [view, setView] = useState<VIEWS>("VIEW_SEARCH");
  const { t } = useTranslation();
  return (
    <PrimeReactProvider>
      <div className="menu">
        <Button onClick={() => setView("VIEW_SEARCH")} title={t("main.toolbar.search")} disabled={view === "VIEW_SEARCH"}><i className="pi pi-search"></i></Button>
        <Button onClick={() => setView("VIEW_TAGS")} title={t("main.toolbar.alts")} disabled={view === "VIEW_TAGS"}><i className="pi pi-tags"></i></Button>
        <Button onClick={() => setView("VIEW_ADD")} title={t("main.toolbar.add")} disabled={view === "VIEW_ADD"}><i className="pi pi-plus"></i></Button>
      </div>
      {view === "VIEW_SEARCH" && <Search/>}
      {view === "VIEW_TAGS" && <TagsAlt/>}
      {view === "VIEW_ADD" && <Add/>}
    </PrimeReactProvider>
  )
}

export default App
