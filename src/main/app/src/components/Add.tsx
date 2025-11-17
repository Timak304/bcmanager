import './Add.css'
import axios from "axios";
import { Button } from "primereact/button";
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from "primereact/inputtextarea";
import { ProgressBar } from 'primereact/progressbar';
import { RadioButton } from "primereact/radiobutton";
import { useCallback, useEffect, useRef, useState } from "react";

type UrlStatus = {
  url: string;
  status: "pending" | "processing" | "ok" | "error" | "already";
  message?: string;
}

const statusBody = (row: UrlStatus) => {
  switch (row.status) {
    case "already": return <i className="pi pi-arrow-right"></i>
    case "ok": return <i className="pi pi-check"></i>
    case "error": return <i className="pi pi-times"></i>
    case "pending": return <i className="pi pi-hourglass"></i>
    case "processing": return <i className="pi pi-spinner"></i>
  }
};

function Add() {
  const [category, setCategory] = useState<"COLLECTION" | "WISHLIST">("COLLECTION");
  const [importEnded, setImportEnded] = useState(false);
  const [ready, setReady] = useState(true);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fanPageRef = useRef<HTMLInputElement>(null);
  const [results, setResults] = useState<UrlStatus[]>([])
  const [currentResultIndex, setCurrentResultIndex] = useState<number | null>(null);
  const [fanPageStatus, setFanPageStatus] = useState({total: 0, current: 0, errors: ""});

  const runImport = useCallback(() => {
    setReady(false);
    setResults([]);
    const urls = [...new Set(inputRef.current?.value.split("\n"))];
    if (urls) {
      const localResults = urls.map((url) => {return {url: url, status: "pending"} as UrlStatus;});
      setResults(localResults);
      setCurrentResultIndex(0);
    }
  }, []);

  const fetchFanPageStatus = useCallback(() => {
    axios.get("/api/fanpage")
        .then(response => {
          setFanPageStatus(response.data);
        });
  }, []);

  useEffect(() => {
    setInterval(fetchFanPageStatus, 5000);
  }, [fetchFanPageStatus]);


  const runImportFanPage = useCallback(() => {
    const fanPage = fanPageRef.current?.value;
    axios.post("/api/fanpage?url=" + fanPage);
  }, []);

  const reset = useCallback(() => {
    setReady(true);
    setImportEnded(false);
  }, []);

  useEffect(() => {
    if (currentResultIndex === null) {
      return;
    }
    const result = results[currentResultIndex];
    if (result.status !== "pending") {
      return;
    }
    result.status = "processing";
    setResults([...results]);
    axios.post("/api/album?url=" + result.url + "&cat=" + category)
      .then(response => {
        if (response.data === "SKIP") {
          result.status = "already";
        }
        else if (response.data === "OK") {
          result.status = "ok";
        }
        else {
          result.status = "error";
          result.message = response.data;
        }
        setResults([...results]);
      })
      .catch(() => {
        result.status = "error";
        setResults([...results]);
      })
      .finally(() => {
        if (results.filter(r => r.status === "pending").length === 0) {
          setImportEnded(true);
          setCurrentResultIndex(null);
        }
        else {
          setCurrentResultIndex(currentResultIndex + 1);
        }
      });
  }, [category, results, currentResultIndex]);

  return (
    <>
      <div>
        Import fan page <InputText ref={fanPageRef} style={{width: "300px"}} placeholder="https://bandcamp.com/fanname" /> <Button onClick={() => runImportFanPage()}>Import</Button>
      </div>
      {(fanPageStatus.errors !== "" || fanPageStatus.total !== 0) &&
        <div style={{margin: "15px"}}>
          <ProgressBar value={(fanPageStatus.current/fanPageStatus.total) * 100} displayValueTemplate={() => fanPageStatus.current + "/" + fanPageStatus.total}></ProgressBar>
          {fanPageStatus.errors}
        </div>
      }
      <hr/>
      <div className="add-toolbar">
        Import manually:
        <div>
          <RadioButton inputId="collection" name="category" value="COLLECTION" onChange={(e) => setCategory(e.value)} checked={category === 'COLLECTION'} />
          <label htmlFor="collection" className="ml-2">Collection</label>
        </div>
        <div>
          <RadioButton inputId="whishlist" name="category" value="WISHLIST" onChange={(e) => setCategory(e.value)} checked={category === 'WISHLIST'} />
          <label htmlFor="whishlist" className="ml-2">Wishlist</label>
        </div>
        <Button disabled={!ready} onClick={() => runImport()}>Import</Button>
        <Button disabled={!importEnded} onClick={reset}>Reset</Button>
      </div>
      {ready && <InputTextarea className="add-textarea" ref={inputRef} placeholder="https://artist.bandcamp.com/album/title
https://otherartist.bandcamp.com/album/othertitle" />}
      {!ready && 
        <table className="add-result-table">
          <thead>
            <tr>
              <th>URL</th>
              <th>Status</th>
              <th>Error</th>
            </tr>
          </thead>
          <tbody>
            {results.map(result => 
               <tr key={result.url}>
                <td>{result.url}</td>
                <td>{statusBody(result)}</td>
                <td>{result.message}</td>
              </tr>
              )
            }
          </tbody>
        </table>
      }
    </>
  );
}
export default Add;