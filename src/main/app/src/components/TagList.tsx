import axios from "axios";
import { useEffect, useState, type SetStateAction } from "react";
import type { TagCount } from "../types/types";
import { TagButton } from "./TagButton";

export function TagList(props: {
    selectedTags: string[];
    showCountOne: boolean;
    showWishlist: boolean;
    tagFilter: string;
    setSelectedTags: (value: SetStateAction<string[]>) => void
}) {
    const [allTags, setAllTags] = useState<TagCount[]>([]);
    
    useEffect(() => {
        axios.get('/api/tags?showWishlist=' + props.showWishlist)
          .then(response => {
            setAllTags(response.data);
          });
      }, [props.showWishlist]);

      return allTags
                .sort((a, b) => b.count - a.count)
                .filter(tag => props.selectedTags.includes(tag.name) || props.showCountOne && tag.count === 1 || tag.count > 1)
                .filter(tag => tag.name.includes(props.tagFilter))
                .map(tag => <TagButton key={tag.name} name={tag.name} count={tag.count} setSelectedTags={props.setSelectedTags} selectedTags={props.selectedTags} />)
}