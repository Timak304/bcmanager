import axios from "axios";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Tag } from "../types/types";
import { useTranslation } from "react-i18next";
import { InputText } from "primereact/inputtext";
import { TagButton } from "./TagButton";
import { Button } from "primereact/button";
import { Toast } from "primereact/toast";

function TagsAlt() {
  const { t } = useTranslation();
  const [tagFilter, setTagFilter] = useState<string>("");
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [baseTag, setBaseTag] = useState<string[]>([]);
  const [altTags, setAltTag] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const toast = useRef<Toast>(null);
  
  const updateTags = useCallback(() => {
    axios.get('/api/tags?showWishlist=true')
      .then(response => {
        setAllTags(response.data);
      });
  }, []);
  
    useEffect(() => {
      updateTags();
    }, [updateTags]);

  const submit = useCallback(() => {
    if (baseTag.length === 0) {
      return;
    }
    setLoading(true);
    axios.post("/api/tagsalt/" + baseTag[0], altTags)
      .then(() => {
        if (toast.current) {
          setLoading(false);
          toast.current.show({ severity: 'success', detail: 'Sucessful' });
          updateTags();
          setBaseTag([]);
          setAltTag([]);
        }
      })
      .catch(error => {
        if (toast.current) {
          toast.current.show({ severity: 'error', summary: 'Error', detail: error.message });
        }
      });
  }, [altTags, baseTag, updateTags]);

  return (
    <div className="tagalt-view">
      <Toast ref={toast} position="bottom-right" />
      <div className="toolbar">
        <InputText value={tagFilter} onChange={(e) => setTagFilter(e.target.value)} placeholder={t("search.toolbar.filterplaceholder")} title={t("search.toolbar.filterinput")} />
        <Button onClick={() => setTagFilter("")} severity="danger"><i className="pi pi-times" /></Button>
      </div>
      <div>
        {
          allTags
            .sort((a, b) => a.name.localeCompare(b.name))
            .filter(tag => {
              const selected = baseTag.includes(tag.name);
              return selected || !selected && baseTag.length === 0 && tag.name.includes(tagFilter);
            })
            .map(tag => <TagButton key={tag.name} count={tag.alternativesAsList.length} name={tag.name} selectedTags={baseTag} setSelectedTags={setBaseTag} />)
        }
      </div>
      <hr/>
      {baseTag.length > 0 && 
        <>
          <div>
            {
              allTags
                .sort((a, b) => a.name.localeCompare(b.name))
                .filter(tag => !baseTag.includes(tag.name) &&  (altTags.includes(tag.name) || tag.name.includes(tagFilter)))
                .map(tag => <TagButton key={tag.name} count={tag.alternativesAsList.length} name={tag.name} selectedTags={altTags} setSelectedTags={setAltTag} />)
            }
          </div>
          <Button disabled={altTags.length === 0 || loading} onClick={submit} style={{marginTop: "15px"}}>{t("tags.button.setalt")}</Button>
        </>
      }
    </div>
  );
}
export default TagsAlt;