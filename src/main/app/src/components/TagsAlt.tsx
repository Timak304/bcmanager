import axios from "axios";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Tag } from "../types/types";
import { useTranslation } from "react-i18next";
import { InputText } from "primereact/inputtext";
import { TagButton } from "./TagButton";
import { Button } from "primereact/button";
import { Toast } from "primereact/toast";
import { Card } from "primereact/card";

type Suggestion = Tag[];

function TagsAlt() {
  const { t } = useTranslation();
  const [tagFilter, setTagFilter] = useState<string>("");
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [baseTag, setBaseTag] = useState<string[]>([]);
  const [altTags, setAltTag] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const toast = useRef<Toast>(null);
  
  const updateTagsAndSuggestions = useCallback(() => {
    axios.get('/api/tags?showWishlist=true')
      .then(response => {
        setAllTags(response.data);
      });
    axios.get("/api/tagsalt/suggestions")
      .then(response => {
        setSuggestions(response.data);
      });
  }, []);
  
    useEffect(() => {
      updateTagsAndSuggestions();
    }, [updateTagsAndSuggestions]);

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
          updateTagsAndSuggestions();
          setBaseTag([]);
          setAltTag([]);
        }
      })
      .catch(error => {
        if (toast.current) {
          toast.current.show({ severity: 'error', summary: 'Error', detail: error.message });
        }
      });
  }, [altTags, baseTag, updateTagsAndSuggestions]);

  return (
    <div className="tagalt-view">
      <Toast ref={toast} position="bottom-right" />
      <div className="toolbar">
        <InputText value={tagFilter} onChange={(e) => setTagFilter(e.target.value)} placeholder={t("search.toolbar.filterplaceholder")} title={t("search.toolbar.filterinput")} />
        <Button onClick={() => setTagFilter("")} severity="danger" style={{margin: "2px 0", padding: "8px"}}><i className="pi pi-times" /></Button>
        <Button disabled={altTags.length === 0 || loading} onClick={submit} style={{marginTop: "15px"}}>{t("tags.button.setalt")}</Button>
      </div>
      <div>
       {
          baseTag.length === 0 && <Card title={t("tags.suggestions.title")}>
            {
              suggestions
                .map(suggestion => 
                  <div>
                    {
                      suggestion
                        .filter(tag => {
                          const selected = baseTag.includes(tag.name);
                          return selected || !selected && baseTag.length === 0 && tag.name.includes(tagFilter);
                        })
                        .map(tag => <TagButton key={tag.name} count={tag.alternativesAsList.length} name={tag.name} selectedTags={baseTag} setSelectedTags={setBaseTag} />)
                    }
                  </div>
                )
            }
          </Card>
        }
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
            <Card title={t("tags.suggestions.title")}>
              {
                suggestions
                  .map(suggestion => 
                    <div>
                      {
                        suggestion
                          .filter(tag => altTags.includes(tag.name) || tag.name.includes(tagFilter))
                          .map(tag => 
                            <TagButton
                              key={tag.name}
                              count={tag.alternativesAsList.length}
                              name={tag.name}
                              selectedTags={altTags}
                              setSelectedTags={setAltTag}
                              title={tag.alternativesAsList.join(", ")}
                              severity={baseTag.includes(tag.name) ? "warning" : undefined}
                            />)
                      }
                    </div>
                  )
              }
            </Card>
            {
              allTags
                .sort((a, b) => a.name.localeCompare(b.name))
                .filter(tag => altTags.includes(tag.name) || tag.name.includes(tagFilter))
                .map(tag =>
                  <TagButton
                    key={tag.name}
                    count={tag.alternativesAsList.length}
                    name={tag.name}
                    selectedTags={altTags}
                    setSelectedTags={baseTag.includes(tag.name) ? () => {} : setAltTag}
                    title={tag.alternativesAsList.join(", ")}
                    severity={baseTag.includes(tag.name) ? "warning" : undefined}
                  />)
            }
          </div>
        </>
      }
    </div>
  );
}
export default TagsAlt;