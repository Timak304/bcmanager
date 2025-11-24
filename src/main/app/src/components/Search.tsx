import { useCallback, useEffect, useState } from 'react'
import axios from 'axios';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import type { Album } from '../types/types';
import { TagList } from './TagList';
import { TagButton } from './TagButton';
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation();

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
        <Button onClick={() => setShowWishlist(!showWishlist)} outlined={!showWishlist} title={t("search.toolbar.wishlistbuttonhelp")}>{t("search.toolbar.wishlistbutton")}</Button>
        <Button onClick={() => setShowCountOne(!showCountOne)} outlined={!showCountOne} title={t("search.toolbar.onebuttonhelp")}>{t("search.toolbar.onebutton")}</Button>
        <InputText value={tagFilter} onChange={(e) => setTagFilter(e.target.value)} placeholder={t("search.toolbar.filterplaceholder")} title={t("search.toolbar.filterinput")} />
        <Button onClick={() => setTagFilter("")} severity="danger" style={{margin: "0 0 2px -8px", padding: "8px 0"}}><i className="pi pi-times" /></Button>
        <Button onClick={() => setSelectedTags([])} severity="danger" title={t("search.toolbar.resethelp")}>{t("search.toolbar.reset")}</Button>
        {albums.length} results
      </div>
      <div className="tag-card">
        <TagList selectedTags={selectedTags} setSelectedTags={setSelectedTags} showCountOne={showCountOne} showWishlist={showWishlist} tagFilter={tagFilter} />
      </div>
      <DataTable value={albums} tableStyle={{ minWidth: '50rem' }} dataKey="id" stripedRows >
        <Column field="category" header={t("search.table.header.type")} body={categoryBody}></Column>
        <Column field="artist" header={t("search.table.header.artist")} sortable></Column>
        <Column field="name" header={t("search.table.header.name")} body={albumNameBody} sortable></Column>
        <Column field="url" header={t("search.table.header.link")} body={linkBodyTemplate}></Column>
        <Column field="location" header={t("search.table.header.location")} sortable></Column>
        <Column field="releaseDate" header={t("search.table.header.releasedate")} sortable></Column>
        <Column field="duration" header={t("search.table.header.duration")} body={durationBody} sortable></Column>
        <Column field="tagList" header={t("search.table.header.tags")} body={tagsBodyTemplate}></Column>
      </DataTable>
    </>
  )
}

export default Search;
