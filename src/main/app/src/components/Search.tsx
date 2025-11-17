import { useCallback, useEffect, useState } from 'react'
import axios from 'axios';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import type { Album } from '../types/types';
import { TagList } from './TagList';
import { TagButton } from './TagButton';

function linkBodyTemplate(album: Album) {
  return <a href={album.url} target='_blank'><i className="pi pi-external-link"></i></a>
}

function categoryBody(album: Album) {
  if (album.category === "WISHLIST") {
    return <i className="pi pi-star" title="Wishlist"></i>;
  }
  else {
    return <i className="pi pi-star-fill" title="Collection"></i>;
  }
}

function durationBody(album: Album) {
  if (!album.duration) {
    return "";
  }
  else {
    return Math.floor(album.duration/60) + ":" + (album.duration % 60)
  }
}

function albumNameBody(album: Album) {
  return <span title={album.trackList}>{album.name}</span>
}

function Search() {
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [albums, setAlbums] = useState<Album[]>([]);
  const [tagFilter, setTagFilter] = useState<string>("");
  const [showCountOne, setShowCountOne] = useState(false);
  const [showWishlist, setShowWishlist] = useState(false);

  useEffect(() => {
    if (selectedTags.length === 0) {
      setAlbums([]);
    }
    else {
      axios.get('/api/albums?tag=' + selectedTags.join("|") + "&showWishlist=" + showWishlist)
        .then(response => {
          setAlbums(response.data);
        });
    }
  }, [selectedTags, showWishlist]);

 const tagsBodyTemplate = useCallback((album: Album) => {
      return album.tagList.map(tag => <TagButton name={tag} selectedTags={selectedTags} setSelectedTags={setSelectedTags} />);
  }, [selectedTags]);

  return (
    <>
      <div className="tool-bar">
        <Button onClick={() => setShowWishlist(!showWishlist)} outlined={!showWishlist} title="Also show albums from wishlist">Show wishlist</Button>
        <Button onClick={() => setShowCountOne(!showCountOne)} outlined={!showCountOne} title="Show tag having only 1 album related">Display 1</Button>
        <InputText value={tagFilter} onChange={(e) => setTagFilter(e.target.value)} placeholder="Filter tag" title="Filter tag list on the expression" />
        <Button onClick={() => setSelectedTags([])} severity="danger" title="Deselect all tags">Reset</Button>
        {albums.length} results
      </div>
      <div className="tag-card">
        <TagList selectedTags={selectedTags} setSelectedTags={setSelectedTags} showCountOne={showCountOne} showWishlist={showWishlist} tagFilter={tagFilter} />
      </div>
      <DataTable value={albums} tableStyle={{ minWidth: '50rem' }} dataKey="id" stripedRows >
        <Column field="category" header="Type" body={categoryBody}></Column>
        <Column field="artist" header="Artist" sortable></Column>
        <Column field="name" header="Name" body={albumNameBody} sortable></Column>
        <Column field="url" header="Link" body={linkBodyTemplate}></Column>
        <Column field="location" header="Location" sortable></Column>
        <Column field="releaseDate" header="Release Date" sortable></Column>
        <Column field="duration" header="Duration" body={durationBody} sortable></Column>
        <Column field="tagList" header="Tag List" body={tagsBodyTemplate}></Column>
      </DataTable>
    </>
  )
}

export default Search;
