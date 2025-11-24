import { Tag, type TagProps } from "primereact/tag";
import type { SetStateAction } from "react";

function toggleTag(selected: string[], tag: string) {
  if (selected.includes(tag)) {
    return selected.filter(e => e !== tag);
  }
  else {
    return selected.concat(tag);
  }
}

export function TagButton(props: {
    name: string;
    count?: number;
    severity?: TagProps["severity"];
    title?: string;
    selectedTags: string[];
    setSelectedTags: (value: SetStateAction<string[]>) => void;
}) {
    return <Tag
            style={{margin: "1px", cursor: "pointer"}}
            value={props.name + (props.count ? " " + props.count : "")}
            key={props.name}
            severity={props.severity ? props.severity : props.selectedTags.includes(props.name) ? "success" : undefined }
            onClick={() => props.setSelectedTags(toggleTag(props.selectedTags, props.name))}
            title={props.title}
          />
}